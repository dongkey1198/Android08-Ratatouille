package com.kdjj.presentation.viewmodel.my

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdjj.domain.model.Recipe
import com.kdjj.domain.model.request.*
import com.kdjj.domain.usecase.UseCase
import com.kdjj.presentation.common.Event
import com.kdjj.presentation.model.MyRecipeItem
import com.kdjj.presentation.model.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MyRecipeViewModel @Inject constructor(
    private val latestRecipeUseCase: UseCase<FetchLocalLatestRecipeListRequest, List<Recipe>>,
    private val favoriteRecipeUseCase: UseCase<FetchLocalFavoriteRecipeListRequest, List<Recipe>>,
    private val titleRecipeUseCase: UseCase<FetchLocalTitleRecipeListRequest, List<Recipe>>,
    private val getRecipeUpdateStateRequest: UseCase<EmptyRequest, Flow<Int>>
) : ViewModel() {

    private val _liveSortType = MutableLiveData<SortType>()
    val liveSortType: LiveData<SortType> get() = _liveSortType

    private val _liveRecipeItemList = MutableLiveData<List<MyRecipeItem>>()
    val liveRecipeItemList: LiveData<List<MyRecipeItem>> get() = _liveRecipeItemList

    private val _liveRecipeItemSelected = MutableLiveData<MyRecipeItem.MyRecipe?>()
    val liveRecipeItemSelected: LiveData<MyRecipeItem.MyRecipe?> get() = _liveRecipeItemSelected

    private val _eventItemDoubleClicked = MutableLiveData<Event<MyRecipeItem.MyRecipe>>()
    val eventItemDoubleClicked: LiveData<Event<MyRecipeItem.MyRecipe>> get() = _eventItemDoubleClicked

    private val _eventSearchIconClicked = MutableLiveData<Event<Unit>>()
    val eventSearchIconClicked: LiveData<Event<Unit>> get() = _eventSearchIconClicked

    private val _eventAddRecipeHasPressed = MutableLiveData<Event<Unit>>()
    val eventAddRecipeHasPressed: LiveData<Event<Unit>> get() = _eventAddRecipeHasPressed

    private val _eventDataLoadFailed = MutableLiveData<Event<Unit>>()
    val eventDataLoadFailed: LiveData<Event<Unit>> get() = _eventDataLoadFailed

    private var isFetching = false

    init {
        setSortType(SortType.SORT_BY_TIME)

        viewModelScope.launch {
            getRecipeUpdateStateRequest(EmptyRequest)
                .onSuccess {
                    it.collect {
                        Log.d("aa", "collect")
                        refreshRecipeList()
                    }
                }
        }
    }

    private fun initFetching() {
        _liveRecipeItemList.value = listOf()
        isFetching = false
    }

    fun setSortType(sortType: SortType) {
        if (_liveSortType.value != sortType) {
            _liveSortType.value = sortType
            initFetching()
            fetchRecipeList(0)
        }
    }

    fun refreshRecipeList() {
        initFetching()
        fetchRecipeList(0)
    }

    fun fetchRecipeList(page: Int) {
        Log.d("aa", "fetchRecipeList")
        if (isFetching && page > 0) return
        isFetching = true

        viewModelScope.launch {
            when (_liveSortType.value) {
                SortType.SORT_BY_TIME ->
                    latestRecipeUseCase(FetchLocalLatestRecipeListRequest(page))
                SortType.SORT_BY_FAVORITE ->
                    favoriteRecipeUseCase(FetchLocalFavoriteRecipeListRequest(page))
                SortType.SORT_BY_NAME ->
                    titleRecipeUseCase(FetchLocalTitleRecipeListRequest(page))
                else -> return@launch
            }.onSuccess { fetchedRecipeList ->
                val myRecipeList = fetchedRecipeList.map { MyRecipeItem.MyRecipe(it) }
                _liveRecipeItemList.value?.let {
                    if (page == 0) {
                        _liveRecipeItemList.value = listOf(MyRecipeItem.PlusButton) + myRecipeList
                    } else {
                        _liveRecipeItemList.value = _liveRecipeItemList.value?.plus(myRecipeList)
                    }
                }
            }.onFailure {
                _liveRecipeItemList.value = listOf()
                _eventDataLoadFailed.value = Event(Unit)
            }
            isFetching = false
        }
    }

//    private fun notifyRecipeDataSetChanged(isSuccess: Boolean) {
//        if (isSuccess) {
//            _liveRecipeItemList.value =
//                listOf(MyRecipeItem.PlusButton) + recipeList
//        } else {
//            _liveRecipeItemList.value = listOf()
//        }
//    }

    fun recipeItemSelected(selectedRecipe: MyRecipeItem.MyRecipe) {
        if (_liveRecipeItemSelected.value != selectedRecipe) {
            _liveRecipeItemSelected.value = selectedRecipe
        } else {
            _eventItemDoubleClicked.value = Event(selectedRecipe)
        }
    }

    fun moveToRecipeEditorActivity() {
        _eventAddRecipeHasPressed.value = Event(Unit)
    }

    fun moveToRecipeSearchFragment() {
        _eventSearchIconClicked.value = Event(Unit)
    }
}
