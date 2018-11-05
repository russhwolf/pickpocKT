package io.intrepid.pickpocket.android

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.russhwolf.settings.PlatformSettings
import io.intrepid.pickpocket.GuessListItem
import io.intrepid.pickpocket.LockViewModel
import io.intrepid.pickpocket.ViewState
import io.intrepid.pickpocket.WebLockProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.AndroidClientEngine
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Suppress("ProtectedInFinal")
class LockActivity : AppCompatActivity(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    private val viewModel: LockArchViewModel by lazy {
        ViewModelProviders.of(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[LockArchViewModel::class.java]
    }

    @BindView(R.id.guess_list)
    protected lateinit var guessList: RecyclerView

    @BindView(R.id.current_guess)
    protected lateinit var currentGuessText: TextView

    @BindViews(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6)
    protected lateinit var buttons: Array<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        ButterKnife.bind(this)

        val adapter = GuessAdapter()
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        guessList.let {
            it.layoutManager = layoutManager
            it.adapter = adapter
        }

        viewModel.state.observe(this, Observer { state ->
            state ?: return@Observer

            buttons.forEach { it.isEnabled = state.enabled }
            currentGuessText.text = state.guess
            currentGuessText.setCompoundDrawablesWithIntrinsicBounds(
                if (state.locked) R.drawable.ic_lock_black_24dp else R.drawable.ic_lock_open_black_24dp,
                0,
                0,
                0
            )
            adapter.submitList(state.results)
        })
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    @OnClick(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6)
    protected fun onButtonClick(button: Button) {
        launch {
            viewModel.lockViewModel.input(
                when (button.id) {
                    R.id.button_1 -> "1"
                    R.id.button_2 -> "2"
                    R.id.button_3 -> "3"
                    R.id.button_4 -> "4"
                    R.id.button_5 -> "5"
                    R.id.button_6 -> "6"
                    else -> throw IllegalArgumentException("Invalid id ${button.id}")
                }
            )
        }
    }

    @OnClick(R.id.reset_button)
    protected fun onResetClick() {
        viewModel.lockViewModel.reset()
    }
}

class GuessAdapter : ListAdapter<GuessListItem, GuessViewHolder>(object : DiffUtil.ItemCallback<GuessListItem>() {
    override fun areItemsTheSame(oldItem: GuessListItem, newItem: GuessListItem): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: GuessListItem, newItem: GuessListItem): Boolean = oldItem == newItem
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuessViewHolder = GuessViewHolder(parent)
    override fun onBindViewHolder(holder: GuessViewHolder, position: Int) = holder.bind(getItem(position))
}

@Suppress("ProtectedInFinal")
class GuessViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.list_item_guess, parent, false)
) {
    @BindView(R.id.guess_value)
    protected lateinit var guessText: TextView
    @BindView(R.id.results_correct)
    protected lateinit var correctText: TextView
    @BindView(R.id.results_misplaced)
    protected lateinit var misplacedText: TextView

    init {
        ButterKnife.bind(this, itemView)
    }

    fun bind(item: GuessListItem) {
        guessText.text = item.guess
        correctText.text = item.numCorrect.toString()
        misplacedText.text = item.numMisplaced.toString()
    }
}

class LockArchViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = PlatformSettings(PreferenceManager.getDefaultSharedPreferences(application))
    val lockViewModel =
        LockViewModel(
            settings = settings,
            lockProvider = WebLockProvider(
                httpClient = HttpClient(AndroidClientEngine(AndroidEngineConfig())) {
                    install(JsonFeature) { serializer = KotlinxSerializer() }
                },
                settings = settings
            )
        )

    private val mutableState = MutableLiveData<ViewState>()
    val state: LiveData<ViewState> get() = mutableState

    init {
        lockViewModel.setListener { mutableState.value = it }
    }

    override fun onCleared() {
        lockViewModel.deinit()
    }
}
