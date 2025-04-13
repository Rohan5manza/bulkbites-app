import React, { useState } from 'react';
import { View, Button, Text } from 'react-native';
import { launchCamera } from 'react-native-image-picker';
import { NativeModules } from 'react-native';

const { FoodDetectorModule } = NativeModules;

const FoodDetect = () => {
  const [label, setLabel] = useState<string | null>(null);
  const [nutritionInfo, setNutritionInfo] = useState<any>(null); // For storing the nutrition information (calories, proteins, etc.)

  const takePhoto = async () => {
    // Launch camera to take a photo of the food
    launchCamera({ mediaType: 'photo' }, async (response) => {
      if (response.assets && response.assets.length > 0) {
        const imagePath = response.assets[0].uri;

        try {
          // Step 1: Send image to YOLO model for food detection
          const foodDetectionResult = await FoodDetectorModule.detectFood(imagePath);
          
          // Step 2: Check if food is detected
          if (foodDetectionResult.isFoodDetected) {
            // If food is detected, send it to the second model for classification
            const classificationResult = await FoodDetectorModule.classifyFood(imagePath);

            // Set the food label and nutritional info
            setLabel(classificationResult.foodLabel);
            setNutritionInfo(classificationResult.nutritionInfo); // Assuming it returns calories, proteins, fats, carbs
          } else {
            // If no food is detected, output a relevant message
            setLabel("No food detected. Please try again.");
            setNutritionInfo(null);
          }
        } catch (error) {
          console.error("Error during detection:", error);
          setLabel("Error during detection");
          setNutritionInfo(null);
        }
      }
    });
  };

  return (
    <View style={{ padding: 16 }}>
      <Button title="Take a Photo to Detect Food" onPress={takePhoto} />
      {label && <Text>Detection Result: {label}</Text>}
      {nutritionInfo && (
        <View>
          <Text>Nutrition Info:</Text>
          <Text>Calories: {nutritionInfo.calories} kcal</Text>
          <Text>Proteins: {nutritionInfo.proteins} g</Text>
          <Text>Fats: {nutritionInfo.fats} g</Text>
          <Text>Carbs: {nutritionInfo.carbs} g</Text>
        </View>
      )}
    </View>
  );
};

export default FoodDetect;
