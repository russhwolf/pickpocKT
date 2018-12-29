package io.intrepid.pickpocket.android

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
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
import io.intrepid.pickpocket.WebLockProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TAG_INPUT_DIALOG = "InputDialog"
private const val TAG_USERS_DIALOG = "UsersDialog"

@Suppress("ProtectedInFinal")
class LockActivity : AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
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
        val usersAdapter = UsersAdapter(this)

        viewModel.state.observe(this, Observer { state ->
            state ?: return@Observer

            startButtons.forEach { it.visibility = if (state.startButtonsVisible) View.VISIBLE else View.GONE }
            resetButton.visibility = if (state.resetButtonVisible) View.VISIBLE else View.INVISIBLE

            buttons.forEach { it.isEnabled = state.enabled }
            currentGuessText.text = state.guess
            currentGuessText.setCompoundDrawablesWithIntrinsicBounds(
                if (state.locked) R.drawable.ic_lock_black_24dp else R.drawable.ic_lock_open_black_24dp,
                0,
                0,
                0
            )
            guessAdapter.submitList(state.results)

            if (state.localConfigVisible) {
                if (findInputDialogFragment() == null) {
                    InputDialogFragment().show(supportFragmentManager, TAG_INPUT_DIALOG)
                }
            } else {
                findInputDialogFragment()?.dismiss()
            }

            val users = state.webConfigOptions
            if (users != null) {
                usersAdapter.setUsers(users)
                if (findUsersDialogFragment() == null) {
                    val usersDialogFragment = UsersDialogFragment()
                    usersDialogFragment.setAdapter(usersAdapter)
                    usersDialogFragment.show(supportFragmentManager, TAG_INPUT_DIALOG)
                }
            } else {
                findUsersDialogFragment()?.dismiss()
            }
        })
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun findInputDialogFragment() =
        supportFragmentManager.findFragmentByTag(TAG_INPUT_DIALOG) as? InputDialogFragment

    private fun findUsersDialogFragment() =
        supportFragmentManager.findFragmentByTag(TAG_USERS_DIALOG) as? InputDialogFragment

    @OnClick(R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6)
    protected fun onButtonClick(button: Button) {
        launch {
            viewModel.lockViewModel.input(
                when (button.id) {
                    R.id.button_1 -> '1'
                    R.id.button_2 -> '2'
                    R.id.button_3 -> '3'
                    R.id.button_4 -> '4'
                    R.id.button_5 -> '5'
                    R.id.button_6 -> '6'
                    else -> throw IllegalArgumentException("Invalid id ${button.id}")
                }
            )
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

class InputDialogFragment : DialogFragment() {
    private val viewModel: LockArchViewModel by lazy {
        ViewModelProviders.of(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireContext().applicationContext as Application)
        )[LockArchViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Code Length")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                viewModel.lockViewModel.selectLocalLength(editText.text.toString())
            }
            .setOnDismissListener {
                viewModel.lockViewModel.dismissLocalLengthInput()
            }
            .create()
    }
}

class UsersDialogFragment : DialogFragment() {
    private var adapter: UsersAdapter? = null

    private val viewModel: LockArchViewModel by lazy {
        ViewModelProviders.of(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(requireContext().applicationContext as Application)
        )[LockArchViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Lock to Pick")
            .setAdapter(adapter) { _, which ->
                viewModel.lockViewModel.selectWebUser(adapter?.getItem(which))
            }
            .setOnDismissListener {
                viewModel.lockViewModel.dismissWebUserInput()
            }
            .create()
    }

    fun setAdapter(adapter: UsersAdapter) {
        this.adapter = adapter
    }
}

class UsersAdapter(context: Context) :
    ArrayAdapter<WebLockProvider.User>(context, android.R.layout.simple_list_item_1) {
    fun setUsers(users: List<WebLockProvider.User>) {
        clear()
        addAll(users)
        notifyDataSetChanged()
    }
}

class LockArchViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = PlatformSettings(PreferenceManager.getDefaultSharedPreferences(application))
    val lockViewModel = LockViewModel(settings = settings)

    private val mutableState = MutableLiveData<ViewState>()
    val state: LiveData<ViewState> get() = mutableState

    init {
        lockViewModel.setListener { mutableState.value = it }
    }

    override fun onCleared() {
        lockViewModel.deinit()
    }
}
