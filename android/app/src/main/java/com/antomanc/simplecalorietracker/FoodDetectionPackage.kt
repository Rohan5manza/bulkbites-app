class FoodDetectionPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext) =
        listOf(FoodDetectionModule(reactContext))

    override fun createViewManagers(reactContext: ReactApplicationContext) = emptyList<ViewManager<*, *>>()
}
