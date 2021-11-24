package com.kdjj.domain.usecase

import com.kdjj.domain.model.Recipe
import com.kdjj.domain.model.request.FetchRecipeTempRequest
import com.kdjj.domain.repository.RecipeImageRepository
import com.kdjj.domain.repository.RecipeTempRepository
import javax.inject.Inject

class FetchRecipeTempUseCase @Inject constructor(
    private val tempRepository: RecipeTempRepository,
    private val imageRepository: RecipeImageRepository
) : ResultUseCase<FetchRecipeTempRequest, Recipe?> {

    override suspend fun invoke(request: FetchRecipeTempRequest): Result<Recipe?> =
        runCatching {
            val recipe = tempRepository.getRecipeTemp(request.recipeId).getOrNull()
                ?: return@runCatching null

            val stepList = recipe.stepList.map { step ->
                if (step.imgPath.isNotEmpty() && !imageRepository.isUriExists(step.imgPath)) {
                    step.copy(imgPath = "")
                } else {
                    step
                }
            }
            recipe.copy(stepList = stepList)
        }
}