# Plan: Improve Model Run Screen UI and Add Presets

## Objective
Improve the layout of the Model Run screen to prevent UI cutoff (specifically the "Properties" button) and add functionality for model configuration presets.

## Key Files & Context
- `app/src/main/java/io/github/xororz/localdream/ui/screens/ModelRunScreen.kt`: Main UI file for the Model Run screen.

## Implementation Steps

### 1. UI Layout Fix
- Wrap the main content container in the `ModelRunScreen` Composable with a `verticalScroll(rememberScrollState())` modifier to ensure all content is reachable on smaller screens.
- Inspect the layout hierarchy to ensure `Column` or `Row` constraints are appropriate.

### 2. Add Preset Functionality
- Define a `ModelPreset` data class to store configuration (e.g., name, steps, cfg, sampler).
- Add a UI component (e.g., a `LazyRow` of chips or a `DropdownMenu`) at the top of the prompt section to select from available presets.
- When a preset is selected, update `generationParamsTmp` (or the corresponding state variables) with the preset's values.

## Verification & Testing
- **UI Layout:** Manually verify that all buttons and labels, including "Properties", are visible on different screen sizes/orientations.
- **Presets:** Verify that selecting a preset correctly updates the input fields for steps, CFG, etc.
- **Generation:** Ensure that generation still works with the applied preset values.
