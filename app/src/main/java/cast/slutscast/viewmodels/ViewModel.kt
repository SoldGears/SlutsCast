package cast.slutscast.viewmodels


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cast.slutscast.models.Model
import cast.slutscast.repository.Chaturbate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewModel : ViewModel() {
    private var items: MutableLiveData<MutableList<Model>> = MutableLiveData()

    private val repo = Chaturbate()

    fun fetchData(url:String, page:Int): MutableLiveData<MutableList<Model>> {
        viewModelScope.launch(Dispatchers.IO) {
            items.postValue(repo.parseCams(url, page))
        }
        return items
    }
}