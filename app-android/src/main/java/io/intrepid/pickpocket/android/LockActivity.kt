package io.intrepid.pickpocket.android

import android.app.Application
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.russhwolf.settings.PlatformSettings
import io.intrepid.pickpocket.GuessListItem
import io.intrepid.pickpocket.LockViewModel
import io.intrepid.pickpocket.ViewState
import io.intrepid.pickpocket.WebLockProvider.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TAG_LOADING_DIALOG = "LoadingDialog"
private const val TAG_INPUT_DIALOG = "InputDialog"
private const val TAG_USERS_DIALOG = "UsersDialog"

@Suppress("ProtectedInFinal")
class LockActivity : AppCompatActivity(), CoroutineScope {

    private val viewModel: LockAndroidViewModel by lazy {
        ViewModelProviders.of(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        ).get<LockAndroidViewModel>()
    }

    override val coroutineContext: CoroutineContext by lazy { viewModel.lockViewModel.coroutineContext }

    @BindView(R.id.lock_name)
    protected lateinit var lockNameText: TextView

    @BindView(R.id.guess_list)
    protected lateinit var guessList: RecyclerView

    @BindView(R.id.current_guess)
    protected lateinit var currentGuessText: TextView

    @BindViews(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6)
    protected lateinit var buttons: Array<Button>

    @BindViews(R.id.start_button_local, R.id.start_button_web)
    protected lateinit var startButtons: Array<Button>

    @BindView(R.id.reset_button)
    protected lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        ButterKnife.bind(this)

        val guessAdapter = GuessAdapter()
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        guessList.let {
            it.layoutManager = layoutManager
            it.adapter = guessAdapter
        }
        val usersAdapter = UsersAdapter(viewModel.lockViewModel::selectWebUser)

        viewModel.state.observe(this, Observer { state ->
            state ?: return@Observer

            startButtons.forEach { it.visibility = if (state.startButtonsVisible) View.VISIBLE else View.GONE }
            resetButton.visibility = if (state.resetButtonVisible) View.VISIBLE else View.INVISIBLE

            buttons.forEach { it.isEnabled = state.enabled }
            currentGuessText.text = state.guess
            lockNameText.text = state.lockName
            lockNameText.setCompoundDrawablesWithIntrinsicBounds(
                if (state.locked) R.drawable.ic_lock_black_24dp else R.drawable.ic_lock_open_black_24dp,
                0,
                0,
                0
            )
            guessAdapter.submitList(state.results)

            updateDialogVisibility(state.loading, TAG_LOADING_DIALOG, ::LoadingDialogFragment)
            updateDialogVisibility(state.localConfigVisible, TAG_INPUT_DIALOG, ::InputDialogFragment)

            val users = state.webUsers
            updateDialogVisibility(users != null, TAG_USERS_DIALOG, ::UsersDialogFragment) {
                usersAdapter.submitList(users ?: emptyList())
                setAdapter(usersAdapter)
            }
        })
    }

    private fun <T : DialogFragment> updateDialogVisibility(
        visible: Boolean,
        tag: String,
        builder: () -> T,
        initializer: T.() -> Unit = {}
    ) {
        @Suppress("UNCHECKED_CAST")
        val dialog = supportFragmentManager.findFragmentByTag(tag) as? T
        if (visible) {
            if (dialog == null) {
                builder()
                    .apply(initializer)
                    .showNow(supportFragmentManager, tag)
            } else {
                dialog.initializer()
            }
        } else {
            dialog?.dismiss()
        }
    }

    @OnClick(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6)
    protected fun onButtonClick(button: Button) {
        launch {
            viewModel.lockViewModel.input(resources.getResourceName(button.id).last())
        }
    }

    @OnClick(R.id.start_button_local)
    protected fun onStartLocalClick() {
        viewModel.lockViewModel.startLocal()
    }

    @OnClick(R.id.start_button_web)
    protected fun onStartWebClick() {
        launch {
            viewModel.lockViewModel.startWeb()
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

class LoadingDialogFragment : DialogFragment() {
    private val viewModel: LockAndroidViewModel by lazy {
        ViewModelProviders.of(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireContext().applicationContext as Application)
        ).get<LockAndroidViewModel>()
    }

    override fun onResume() {
        super.onResume()
        viewModel.state.observe(viewLifecycleOwner, Observer {
            if (!it.loading) dialog?.cancel()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ProgressBar(requireContext())
    }
}

class InputDialogFragment : DialogFragment() {
    private val viewModel: LockAndroidViewModel by lazy {
        ViewModelProviders.of(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireContext().applicationContext as Application)
        ).get<LockAndroidViewModel>()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Code Length")
            .setView(editText)
            .setPositiveButton("OK") { _, _ -> viewModel.lockViewModel.selectLocalLength(editText.text.toString()) }
            .setNegativeButton("Cancel") { _, _ -> dialog.cancel() }
            .create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        viewModel.lockViewModel.dismissLocalLengthInput()
        super.onCancel(dialog)
    }
}

class UsersDialogFragment : DialogFragment() {
    private val viewModel: LockAndroidViewModel by lazy {
        ViewModelProviders.of(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireContext().applicationContext as Application)
        ).get<LockAndroidViewModel>()
    }

    private var adapter: UsersAdapter? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val recyclerView = RecyclerView(requireContext()).also {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = adapter
            it.id = android.R.id.list
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Lock to Pick")
            .setView(recyclerView)
            .setNegativeButton("Cancel") { _, _ -> dialog.cancel() }
            .create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        viewModel.lockViewModel.dismissWebUserInput()
        super.onCancel(dialog)
    }

    override fun onResume() {
        super.onResume()
        val recyclerView = dialog.findViewById<RecyclerView>(android.R.id.list)
        recyclerView.adapter = adapter
    }

    fun setAdapter(adapter: UsersAdapter) {
        this.adapter = adapter
    }
}

class UsersAdapter(private val action: (User) -> Unit) :
    ListAdapter<User, UserViewHolder>(object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder = UserViewHolder(parent, action)
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) = holder.bind(getItem(position))
}

@Suppress("ProtectedInFinal")
class UserViewHolder(parent: ViewGroup, action: (User) -> Unit) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
) {
    @BindView(android.R.id.text1)
    protected lateinit var textView: TextView

    private var item: User? = null

    init {
        ButterKnife.bind(this, itemView)
        itemView.setOnClickListener { item?.apply(action) }
    }

    fun bind(item: User) {
        this.item = item
        textView.text = item.toString()
    }
}

class LockAndroidViewModel(application: Application) : AndroidViewModel(application) {
    val lockViewModel: LockViewModel

    private val mutableState = MutableLiveData<ViewState>()
    val state: LiveData<ViewState> get() = mutableState

    init {
        val settings = PlatformSettings(PreferenceManager.getDefaultSharedPreferences(application))
        lockViewModel = LockViewModel(settings) { mutableState.value = it }
    }

    override fun onCleared() {
        lockViewModel.deinit()
    }
}
