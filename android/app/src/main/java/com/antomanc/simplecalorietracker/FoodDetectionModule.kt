@ReactModule(name = FoodDetectionModule.NAME)
class FoodDetectionModule(reactContext: ReactApplicationContext)
    : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = NAME

    private val ortEnv = OrtEnvironment.getEnvironment()
    private val ortSession = ortEnv.createSession(
        assetFilePath(reactContext, "best.onnx"),
        OrtSession.SessionOptions()
    )

    private val pytorchModule = LiteModuleLoader.load(assetFilePath(reactContext, "mobile.ptl"))

    @ReactMethod
    fun analyzeImage(base64Image: String, promise: Promise) {
        try {
            val bitmap = base64ToBitmap(base64Image)

            val isFood = detectObjects(bitmap)
            if (!isFood) {
                promise.resolve("No food detected")
                return
            }

            val label = classifyFood(bitmap)
            promise.resolve(label)
        } catch (e: Exception) {
            promise.reject("Error", e.message)
        }
    }

    companion object {
        const val NAME = "FoodDetection"
    }

    // Add utility functions here

    fun base64ToBitmap(base64Str: String): Bitmap {
    val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}

fun preprocessForYOLO(bitmap: Bitmap): OnnxTensor {
    val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

    val inputTensor = FloatArray(1 * 3 * 640 * 640)
    val pixels = IntArray(640 * 640)
    resized.getPixels(pixels, 0, 640, 0, 0, 640, 640)

    for (i in 0 until 640 * 640) {
        val pixel = pixels[i]
        inputTensor[i] = ((pixel shr 16 and 0xFF) / 255.0f)  // R
        inputTensor[i + 640 * 640] = ((pixel shr 8 and 0xFF) / 255.0f)  // G
        inputTensor[i + 2 * 640 * 640] = ((pixel and 0xFF) / 255.0f)  // B
    }

    val shape = longArrayOf(1, 3, 640, 640)
    return OnnxTensor.createTensor(ortEnv, inputTensor, shape)
}
fun detectObjects(bitmap: Bitmap): Boolean {
    val inputTensor = preprocessForYOLO(bitmap)
    val results = ortSession.run(mapOf("images" to inputTensor))
    val outputTensor = results[0].value as Array<Array<FloatArray>> // shape: [1, N, 85]

    for (det in outputTensor[0]) {
        val classId = det[5].toInt()
        val confidence = det[4]
        if (confidence > 0.5) {
            return true // food detected
        }
    }
    return false
}
fun classifyFood(bitmap: Bitmap): String {
    val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
    val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
        resized,
        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
        TensorImageUtils.TORCHVISION_NORM_STD_RGB
    )

    val output = pytorchModule.forward(IValue.from(inputTensor)).toTensor()
    val scores = output.dataAsFloatArray
    val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: -1

    val labels = listOf("Pizza", "Burger", "Sushi", "Fries") // Add all classes in order
    return if (maxIdx != -1) labels[maxIdx] else "Unknown"
}

}

@ReactModule(name = FoodDetectionModule.NAME)
class FoodDetectionModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = NAME

    @ReactMethod
    fun analyzeImage(base64Image: String, promise: Promise) {
        try {
            val bitmap = base64ToBitmap(base64Image)

            val isFood = detectObjects(bitmap)
            if (!isFood) {
                promise.resolve("No food detected")
                return
            }

            val label = classifyFood(bitmap)
            promise.resolve(label)
        } catch (e: Exception) {
            promise.reject("Error", e.message)
        }
    }

    companion object {
        const val NAME = "FoodDetection"
    }
}
