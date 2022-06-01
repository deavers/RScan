package com.example.rscan

// Необъходимые для сканера объекта библиотеки
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_object_detection.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ObjectDetection : AppCompatActivity(), ImageAnalysis.Analyzer {

    companion object {
        // Константа для объявление неизменной переменной для последущего вызова по данному тегу
        private const val TAG = "ObjectDetection"
        // Константы которые определяют данные для запроса разрешение камеры
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        // Инициализация библиотеки который необъходим для работы с Tensorflow
        init {
            System.loadLibrary("native-lib")
        }
    }

    private lateinit var cameraExecutor: ExecutorService // Переменная для определения фиксированного потока информации с камеры (видео ирл)
    private var imageAnalyzer: ImageAnalysis? = null // Переменная анализатора картинки (видео)
    private var detectorAddr = 0L
    private lateinit var nv21: ByteArray
    private val labelsMap = arrayListOf<String>() // Массив базы данных из списка
    private val _paint = Paint() // Переменная для рисования квадрата определённого объекта

    // Функция при запуске данного экрана
    override fun onCreate(savedInstanceState: Bundle?) {
        // Нужная фукнция при создание, которая обеспечивает сохранение предыдущей информации (ориентации камеры)
        super.onCreate(savedInstanceState)
        // Открытие шаблона сканера для объекта
        setContentView(R.layout.activity_object_detection)

        // Если разрешения все разрешение предоставлены тогда идёт запуск камеры
        if (allPermissionsGranted()) {
            startCamera()
        }
        // Иначе запрос на разрешения камеры
        else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Для работы CameraX необходим фиксированный поток для выполнение камеры
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Инициализация краски для отображение обнаруженного объекта на камере
        _paint.color = Color.RED // Цвет краски - красный
        _paint.style = Paint.Style.STROKE // Стиль краски - строчный
        _paint.strokeWidth = 3f // Ширина строки - 3 пикселя (f - float)
        _paint.textSize = 50f // Размер текста - 50 пикселей (f - float)
        _paint.textAlign = Paint.Align.LEFT // Расположение текста - по левой стороне

        // Установка нарисованных объектов поверх всего
        surfaceView.setZOrderOnTop(true)
        // Установка формы поверхности
        surfaceView.holder.setFormat(PixelFormat.TRANSPARENT)
        // Выполнения функции загрузки примеров
        loadLabels()
    }

    // Приватная функция для вызова разрешения на камеру
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Функция для определения результата разрешение камеры в приложении
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Если результат соответствует коду для запроса то
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Если все разрешения даны тогда идёт запуск камеры
            if (allPermissionsGranted()) {
                startCamera()
            }
            // Иначе вывод окна с текстом что нету доступа и после закрывается данный экран с камерой
            else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Функция запуска камеры
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Разворот камеры в нужное положение
            val rotation = viewFinder.display.rotation

            // Предпоказ камеры и предворительная смена ориентации камеры
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Анализатор изображения
            imageAnalyzer = ImageAnalysis.Builder()
                // Стандартное значение камеры
                .setTargetResolution(Size(768, 1024))
                // Поворот камеры
                .setTargetRotation(rotation)
                // Нужная для прямого вывода без задержок видео
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                // Создать
                .build()
                // После анализатор изображений будет назначен нашей камере
                .also {
                    it.setAnalyzer(cameraExecutor, this)
                }

            // Вывод ошибки
            try {
                // Разбинд части CameraX интерфейса
                cameraProvider.unbindAll()

                // Бинд части CameraX интерфейса
                cameraProvider.bindToLifecycle(
                    this,
                    // CameraX позволяет обрабатывать без помощи кнопок
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    // Показать анализатор изображения
                    preview,
                    imageAnalyzer
                )
                    // Вывод ошибки если бинд части интерфейса не получился
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Анализ уже по базе
    override fun analyze(image: ImageProxy) {
        // Если размеры массива пикселей картинки меньше трёх
        if (image.planes.size < 3) {return}
        //
        if (detectorAddr == 0L) {
            detectorAddr = initDetector(this.assets)
        }

        // Переменная для изменения ориентации объекта по градусами кадра
        val rotation = image.imageInfo.rotationDegrees

        // Переменная плоскости пикселей в картинке
        val planes = image.planes

        // Переменные для сохранения YUV модели в буфер (пиксели)
        val yBuffer = planes[0].buffer  // Y - Яркость

        // U - цветоразные компоненты
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        // Переменные для сохранения YUV модели лимита каждого буфера (пиксели)
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        // Для прорисовки квадрата по начальной точке которая совпадает и конечной которая
        // заканчивает данный объект

        if (!::nv21.isInitialized) {
            nv21 = ByteArray(ySize + uSize + vSize)
        }

        // Если U или V поменялись местами, так как часто результаты выводились не точно
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        // Запись результата в переменную если определился объект по функцией определения
        val res = detect(detectorAddr, nv21, image.width, image.height, rotation)



        // Переменная для сохранения поверхности камеры для дальнейшей прорисовки объекта
        val canvas = surfaceView.holder.lockCanvas()
        // Если переменная не равна нулю тогда
        if (canvas != null) {
            // Создается цвет холста
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY)

            // Прорисовка объекта пока результат не определился
            for (i in 0 until res[0].toInt()) {
                // Прорисовка обнаруженного объекта по холсту (кадру), по размера квадрата,
                // по развороту в нужном направлений, по результату найденного объекта в массиве и переменная for
                this.drawDetection(canvas, image.width, image.height, rotation, res, i)
            }
            // Разблокировка холста для работоспособности вывода результата
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
        // Закрыть картинку
        image.close()
    }

    // Функция для рисования результата
    private fun drawDetection(
        canvas: Canvas, // Холст который сохранён как кадр для сохранения местоположения объекта
        frameWidth: Int, // Ширина кадра
        frameHeight: Int, // Высота кадра
        rotation: Int, // Поворот в нужную сторону объекта
        detectionsArr: FloatArray, // Переменная массив которая перебирает объекты в базе
        detectionIdx: Int // Переменная которая определяет номер объекта в базе
    ) {
        // Переменная для опеределения по размеру объекта в базе
        val pos = detectionIdx * 6 + 1
        // Переменная процента схожести объекта
        val score = detectionsArr[pos + 0]
        // Переменная номера объекта в базе
        val classId = detectionsArr[pos + 1]
        // Переменный для определения начальной и конечной точки объекта
        var xmin = detectionsArr[pos + 2]
        var ymin = detectionsArr[pos + 3]
        var xmax = detectionsArr[pos + 4]
        var ymax = detectionsArr[pos + 5]

        // Фильтрация по схожести процента
        if (score < 0.4) return

        // Переменные для сохранения размеров рамки (высота и ширина)
        val w = if (rotation == 0 || rotation == 180) frameWidth else frameHeight
        val h = if (rotation == 0 || rotation == 180) frameHeight else frameWidth

        // Координаты на экране, определенного объекта на холсте базы обнаруженого объекта
        val scaleX = viewFinder.width.toFloat() / w
        val scaleY = viewFinder.height.toFloat() / h

        // Смещения камеры на экране
        val xoff = 0 // viewFinder.left.toFloat()
        val yoff = 0 // viewFinder.top.toFloat()

        // Изменения координат начальной и конечной точки координат путём определения координаты на экране холста и смещения камеры
        xmin = xoff + xmin * scaleX
        xmax = xoff + xmax * scaleX
        ymin = yoff + ymin * scaleY
        ymax = yoff + ymax * scaleY


        // Рисование квадрата на новом холсте ирл камеры
        val p = Path()
        p.moveTo(xmin, ymin)
        p.lineTo(xmax, ymin)
        p.lineTo(xmax, ymax)
        p.lineTo(xmin, ymax)
        p.lineTo(xmin, ymin)

        // Рисование на холсте уже с назначенными координатами
        canvas.drawPath(p, _paint)

        // Запись названия объекта с совподающей базы
        val label = labelsMap[classId.toInt()]

        // Переменная текста над квадратом обнаруженного объекта
        val txt = "%s (%.2f)".format(label, score)
        canvas.drawText(txt, xmin, ymin, _paint)

    }

    // Функция для загрузки объектов в ирл
    private fun loadLabels() {
        val labelsInput = this.assets.open("labels.txt")
        val br = BufferedReader(InputStreamReader(labelsInput))
        var line = br.readLine()
        while (line != null) {
            labelsMap.add(line)
            line = br.readLine()
        }

        br.close()
    }

    private external fun initDetector(assetManager: AssetManager?): Long
    private external fun destroyDetector(ptr: Long)
    private external fun detect(ptr: Long, srcAddr: ByteArray, width: Int, height: Int, rotation: Int): FloatArray
}