/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
)

package com.bilabila.mizuki_ui

import AppTheme
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.trimmedLength
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
//import com.bilabila.mizuki_ui.theme.AppTheme
import com.maxkeppeker.sheets.core.models.base.SelectionButton
import com.maxkeppeker.sheets.core.models.base.rememberSheetState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.calendar.models.CalendarStyle
import com.maxkeppeler.sheets.calendar.models.CalendarTimeline
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

const val disable_persist = true

object ROUTE {
    const val home = "home"
    const val account = "account"
    const val help = "help"
}

class NavigationActions(navController: NavHostController) {
    val navigateToHome: () -> Unit = {
        navController.navigate(ROUTE.home) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
    val navigateToAccount = { id: String ->
        navController.navigate(ROUTE.account + "/" + id) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
    val navigateToHelp = {
        navController.navigate(ROUTE.help) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
}

@Composable
fun App(
    repo: Repo,
) {
    // status bar color sync: https://google.github.io/accompanist/systemuicontroller/
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()
    DisposableEffect(systemUiController, useDarkIcons) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
        onDispose {}
    }

    // router
    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        NavigationActions(navController)
    }

    AppTheme {
        NavHost(
            navController = navController,
            startDestination = ROUTE.home
        ) {
            composable(
                ROUTE.account + "/{id}",
            ) { backStackEntry ->
                val id = (backStackEntry.arguments?.getString("id")) ?: ""
                AccountView(
                    viewModel = viewModel(factory = AccountViewModel.factory(repo, id)),
                    navigationToHome = navigationActions.navigateToHome,
                )
            }
            composable(ROUTE.home) {
                HomeView(
                    viewModel = viewModel(factory = HomeViewModel.factory(repo)),
                    navigationToAccount = navigationActions.navigateToAccount,
                    navigationToHelp = navigationActions.navigateToHelp,
                )
            }
            composable(ROUTE.help) {
                HelpView(
                    navigationToHome = navigationActions.navigateToHome,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpView(navigationToHome: () -> Unit) {
    BackHandler(true, navigationToHome)
    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        bottomBar = {
            Divider(color = Color.Transparent) // to overcome wrong imePadding on bottom text field
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "帮助",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigationToHome,
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // TODO display some info like resolution, android version, service status
                // may be get from rust
                item {
                    Text(
                        "设置构成",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    Text("基础设置 + 账号N修改项 + 覆盖设置修改项 = 账号N设置")
                }
                item {
                    Text(
                        "任务队列",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    Text(
                        "每个账号的日常与非日常任务根据历史完成时间在各自定时点加入队列。队列中任务按“优先级越高越先，优先级相同则入队时间越早越先”排序。任何时刻只有最先任务处于执行状态。日常任务默认优先级50，非日常10。\n\n" +
                                "案例：\n" +
                                "1. 账号N一天双清，20点到24点禁止登录，周末不打 => 间隔小时12，优先级51，首次时间1点，禁用星期选周六日\n" +
                                "2. 账号N尽快打 => 临时优先级99\n" +
                                "3. 账号N别影响其他账号 => 临时优先级49\n" +
                                "4. 账号N别打 => 禁用星期全选\n" +
                                "5. 账号N两天后再打 => 调整允许日期\n" +
                                "6. 账号N肉鸽任务别影响日常 => 非日常优先级10\n" +
                                "7. 账号N肉鸽任务需要尽快打 => 日常优先级99，非日常优先级98\n"
                    )
                }
            }
        }
    }
}


class HomeViewModel(
    val repo: Repo
) : ViewModel() {
    val allAccount = repo.getAllAccount()
    val _state = MutableStateFlow(emptyList<Account>())
    val state = _state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), _state.value)


    fun updateFunc(id: String): (((Account) -> Account) -> Unit) {
        return { change ->
            allAccount.update { it ->
                val list = it.toMutableList()
                val index = list.indexOfFirst { it.id == id }
                if (index >= 0) {
                    list[index] = change(list[index])
                }
                list
            }
            repo.save()
        }
    }

    fun addAccount(): Int {
        allAccount.update {
            it + listOf(defaultAccount(it.size))
        }
        repo.save()
        return allAccount.value.size

    }

    fun import(content: String): Boolean {
        try {
            val new = Json.decodeFromString<List<Account>>(content)
            allAccount.update {
                new
            }
            repo.save()
            return true
        } catch (e: SerializationException) {
            return false
        }
    }

    fun export(stream: OutputStream): Boolean {
        return try {
            stream.write(Json.encodeToString(allAccount.value).toByteArray())
            true
        } catch (e: IOException) {
            false
        }
    }

    init {
        Log.d("UUUU", "home view model init")
        viewModelScope.launch {
            allAccount.collect { new ->
                _state.update {
                    new
                }
            }
        }
    }

    companion object {
        fun factory(
            repo: Repo,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repo) as T
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    viewModel: HomeViewModel,
    navigationToAccount: (id: String) -> Unit,
    navigationToHelp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val before = state.getOrElse(0) { Account() }
    val after = state.getOrElse(1) { Account() }
    val other = state.slice(2 until state.size)

    var showAddChoice by remember {
        mutableStateOf(false)
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var message by remember {
        mutableStateOf("")
    }
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    val context = LocalContext.current
    val import =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
            var success = false
            result?.let {
                context.contentResolver.openInputStream(result)?.let { stream ->
                    val bytes = stream.readBytes()
                    if (viewModel.import(bytes.decodeToString())) {
                        success = true
                    }
                    stream.close()
                }
            }
            message = "导入" + if (success) "成功" else "失败"
        }

    val export =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/json")) { result ->
            var success = false
            result?.let {
                context.contentResolver.openOutputStream(result)?.let { stream ->
                    if (viewModel.export(stream)) {
                        success = true
                    }
                    stream.close()
                }
            }
            message = "导出" + if (success) "成功" else "失败"
        }


    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        bottomBar = {
            Divider(color = Color.Transparent) // to overcome wrong imePadding on bottom textfield
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mizuki",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    DropdownMenu(
                        expanded = showAddChoice,
                        onDismissRequest = { showAddChoice = false }) {
                        DropdownMenuItem(text = { Text("新增账号") }, onClick = remember {
                            {
                                val size = viewModel.addAccount()
                                message = "账号总数：" + (size - 2)
                            }
                        })
                        DropdownMenuItem(text = { Text("从文件导入") }, onClick = remember {
                            {
                                showAddChoice = false
                                import.launch("application/json")

                            }
                        })
                        DropdownMenuItem(text = { Text("导出到文件") }, onClick = remember {
                            {
                                showAddChoice = false
                                export.launch("mizuki.json")
                            }
                        })
                    }
                    IconButton(onClick = {
                        showAddChoice = !showAddChoice
                    }) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = navigationToHelp) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    Icons.Filled.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row {
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            ), shape = RoundedCornerShape(16.dp),
                            onClick = remember {
                                {
                                    navigationToAccount("before")
                                }
                            }, modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                "基础设置",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ), shape = RoundedCornerShape(16.dp),
                            onClick = remember {
                                {
                                    navigationToAccount("after")
                                }
                            }, modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                "覆盖设置",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                            )
                        }
                    }
                }

                itemsIndexed(other, key = { index, account -> account.id }) { index, account ->
                    Surface(shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        onClick = remember {
                            {
                                navigationToAccount(account.id)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 0.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp
                            )
                        ) {
                            if (account.note.trimmedLength() > 0) {
                                Row {
                                    Text(
                                        account.note,
                                        style = MaterialTheme.typography.titleLarge,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 5,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Text(
                                    account.server + " " + account.username,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = account.lastEndTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AccountItemView(
                                account,
                                viewModel.updateFunc(account.id),
                                before,
                                after,
                                lite = true
                            )
                        }
                    }
                }
            }
        }


    }
}

@Serializable
data class Account(
    // always unique
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val server: String = "官服",
    val evaluating: Boolean = false,
    val note: String = "",

    // overridable
    val priority: String = "50",
    val priority_override: Boolean = false,
    val fight: String = "jm hd ce ls",
    val fight_override: Boolean = false,
    val max_drug: String = "0",
    val max_drug_override: Boolean = false,
    val max_drug_day: String = "1 0 1 1 1 2 3 99",
    val max_drug_day_override: Boolean = false,
    val max_stone: String = "0",
    val max_stone_override: Boolean = false,
    val fight_activity_shop: Boolean = true,
    val fight_activity_shop_override: Boolean = false,
    val prefer_goods: String = "",
    val prefer_goods_override: Boolean = false,
    val forbid_goods: String = "",
    val forbid_goods_override: Boolean = false,
    val auto_recruit: String = "0 1 2 3 4 5 6 7 8 9",
    val auto_recruit_override: Boolean = false,
    val job_mail: Boolean = true,
    val job_mail_override: Boolean = false,
    val job_fight: Boolean = true,
    val job_fight_override: Boolean = false,
    val job_dorm: Boolean = true,
    val job_dorm_override: Boolean = false,
    val job_dorm_item: String = "0 1 2 3 4 5 6 7 8 9",
    val job_dorm_item_override: Boolean = false,
    // val job_friend: Boolean = true,
    // val job_friend_override: Boolean = false,
    // val job_gain: Boolean = true,
    // val job_gain_override: Boolean = false,
    // val job_shift: Boolean = true,
    // val job_shift_override: Boolean = false,
    // val job_manu: Boolean = false,
    // val job_manu_override: Boolean = false,
    // val job_clue: Boolean = false,
    // val job_clue_override: Boolean = false,
    // val job_assist: Boolean = false,
    // val job_assist_override: Boolean = false,
    val job_shop: Boolean = true,
    val job_shop_override: Boolean = false,
    val job_recruit: Boolean = true,
    val job_recruit_override: Boolean = false,
    val job_task: Boolean = true,
    val job_task_override: Boolean = false,
    val job_activity_checkin: Boolean = true,
    val job_activity_checkin_override: Boolean = false,
    val job_activity_recruit: Boolean = true,
    val job_activity_recruit_override: Boolean = false,
    // val datetime_control: Boolean = false,
    // val datetime_control_override: Boolean = false,
    val allow_begin_date: String = "",
    val allow_begin_date_override: Boolean = false,
    val allow_end_datetime: String = "",
    val allow_end_datetime_override: Boolean = false,
    val forbid_weekday: String = "",
    val forbid_weekday_override: Boolean = false,
    // val job_checkin: Boolean = false,
    // val job_checkin_override: Boolean = false,
    val give_away_all_clue: Boolean = false,
    val give_away_all_clue_override: Boolean = false,
    val crontab_start: String = "04:00",
    val crontab_start_override: Boolean = false,
    val crontab_step: String = "8",
    val crontab_step_override: Boolean = false,
    val fight_max_failed_times: String = "2",
    val fight_max_failed_times_override: Boolean = false,
    val login_max_see_times: String = "3",
    val login_max_see_times_override: Boolean = false,
    val captcha_username: String = "",
    val captcha_username_override: Boolean = false,
    val captcha_password: String = "",
    val captcha_password_override: Boolean = false,

    // 非日常
    val fight_pass: String = "",
    val fight_pass_override: Boolean = false,

    // generated
    val lastEndTime: String = "",

    )

fun defaultAccount(i: Int): Account {
    val id = when (i) {
        0 -> "before"
        1 -> "after"
        else -> UUID.randomUUID().toString()
    }
    val note = when (i) {
        0 -> "基础设置"
        1 -> "覆盖设置"
        else -> (i - 1).toString()
    }
    return Account(
        id = id,
        note = note,
    )

}

class Repo(val activity: Activity) {
    companion object {
        // repo will be reinit on MainActivity recreate, use Singleton
        val allAccount = MutableStateFlow(emptyList<Account>())
    }

    fun save_config(key: String, value: String) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    fun load_config(key: String, default: String): String {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return default
        return sharedPref.getString(key, default)!!
    }

    // just load all into memory, as its small
    // room migration is complex
    fun save() {
        save_config("account", Json.encodeToString(allAccount.value))
    }

    fun load(): List<Account> {
        if (disable_persist) return (0..5).map { i ->
            defaultAccount(i)
        }

        val content = load_config("account", "")
        return try {
            Json.decodeFromString(content)
        } catch (e: SerializationException) {
            (0..5).map { i ->
                defaultAccount(i)
            }
        }
    }

    fun getAllAccount(): MutableStateFlow<List<Account>> {
        return allAccount
    }

    init {
        Log.d("UUUU", "repo init")
        allAccount.update {
            load()
        }
    }
}

class AccountViewModel(
    val repo: Repo,
    val id: String
) : ViewModel() {
    val allAccount = repo.getAllAccount()

    // dynamic
    val _state = MutableStateFlow(Account())
    val state = _state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value
    )

    // assume static
    val before = allAccount.value[0]
    val after = allAccount.value[1]

    init {
        Log.d("UUUU", "account view model init")
        viewModelScope.launch {
            allAccount.mapNotNull { it -> it.find { it.id == id } }.collect { new ->
                _state.update {
                    new
                }
            }
        }
    }

    fun save() {
        allAccount.update { it ->
            val list = it.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index >= 0) {
                list[index] = _state.value
            }
            list
        }
        repo.save()
    }

    companion object {
        fun factory(
            repo: Repo, id: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {
                return AccountViewModel(repo, id) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountView(
    viewModel: AccountViewModel,
    navigationToHome: () -> Unit,
) {
    val account by viewModel.state.collectAsStateWithLifecycle()
    val update = viewModel._state::update
    val before = viewModel.before
    val after = viewModel.after
    val id = viewModel.id
    val before_account = id == "before"
    val after_account = id == "after"

    val back = remember {
        {
            viewModel.save()
            navigationToHome()
        }
    }

    BackHandler(true, back)

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        bottomBar = {
            Divider(color = Color.Transparent) // to overcome wrong imePadding on bottom textfield
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = account.note,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = back,
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(scaffoldPadding)
        ) {
            AccountItemView(account, update, before, after, before_account, after_account)
        }
    }


}


@Composable
fun AccountItemView(
    account: Account,
    update: ((Account) -> Account) -> Unit,
    before: Account,
    after: Account,
    before_account: Boolean = false,
    after_account: Boolean = false,
    lite: Boolean = false,
) {
    val animationScope = rememberCoroutineScope()
    fun enabled(prefer_before: Boolean, prefer_after: Boolean): Boolean =
        if (before_account) {
            true
        } else if (after_account) {
            !prefer_before
        } else {
            !prefer_before && !prefer_after
        }

    fun <T> content(
        prefer_before: Boolean,
        prefer_after: Boolean,
        before: T,
        current: T,
        after: T
    ): T = if (before_account) {
        current
    } else if (after_account) {
        if (prefer_before) {
            before
        } else {
            current
        }
    } else {
        if (prefer_after) {
            after
        } else if (prefer_before) {
            before
        } else {
            current
        }
    }

    fun see(
        prefer_before: Boolean,
        prefer_after: Boolean,
        before: Boolean,
        current: Boolean,
        after: Boolean
    ): Boolean =
        before_account || after_account || content(
            prefer_before,
            prefer_after,
            before,
            current,
            after
        )

    @Composable
    fun Enabled(
        enabled: Boolean, prefer_before: Boolean, prefer_after: Boolean,
        prefer_change: (account: Account, value: Boolean) -> Account, disable_prefer: Boolean,
    ) {
        if (disable_prefer || before_account) {
            Spacer(modifier = Modifier.width(16.dp))
        } else if (after_account) {
            Checkbox(
                checked = !prefer_before,
                onCheckedChange = remember {
                    { value ->
                        update {
                            prefer_change(it, value)
                        }
                    }
                },
            )
        } else {
            Checkbox(
                enabled = !prefer_after,
                checked = !prefer_before || prefer_after,
                onCheckedChange = remember {
                    { value ->
                        update {
                            prefer_change(it, value)
                        }
                    }
                },
            )
        }
    }

    @Composable
    fun Label(enabled: Boolean, label: String) {
        Text(
            label,
            modifier = Modifier
                .alpha(
                    if (enabled)
                        1f else 0.5f
                )
                .padding(end = 16.dp)
        )
    }

    @Composable
    fun Section(label: String) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 12.dp, top = 32.dp, bottom = 8.dp)
        )
    }

    // afraid kotlin reflection performance
    @Composable
    fun Item(
        label: String = "",
        before: String = "",
        current: String = "",
        after: String = "",
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: String) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
        tailingIcon: @Composable (() -> Unit)? = null,
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            TextField(
                modifier = Modifier
                    .weight(1f),
                value = content,
                onValueChange = remember {
                    { value ->
                        update {
                            change(it, value)
                        }
                    }
                },
                enabled = enabled,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                ),
                trailingIcon = tailingIcon
            )
        }
    }

    @Composable
    fun Item(
        label: String = "",
        before: Boolean = false,
        current: Boolean = false,
        after: Boolean = false,
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)
        Row(
            verticalAlignment = Alignment.CenterVertically

        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = content,
                onCheckedChange = remember {
                    { value ->
                        update {
                            change(it, value)
                        }
                    }
                },
                enabled = enabled,
            )
        }
    }

    @Composable
    fun ItemDate(
        label: String = "",
        before: String = "",
        current: String = "",
        after: String = "",
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: String) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)
        Row(
            verticalAlignment = Alignment.CenterVertically

        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            Spacer(modifier = Modifier.weight(1f))

            val now = LocalDate.now()
            val initialDate: LocalDate = try {
                val saved = LocalDate.parse(content)
                if (saved.isBefore(now)) {
                    now
                } else {
                    saved
                }
            } catch (e: DateTimeParseException) {
                now
            }
            var date by remember {
                mutableStateOf(initialDate)
            }
            val sheetState = rememberSheetState(
                visible = false,
                embedded = false,
                onCloseRequest = remember {
                    {
                        update {
                            change(it, date.toString())
                        }
                    }
                })

            TextButton(
                shape = RoundedCornerShape(16.dp),

                onClick = { sheetState.show() }, enabled = enabled
            ) {
                Text("$initialDate 起")
            }
// TODO try popular calendar lib
            CalendarDialog(
                state = sheetState,
                config = CalendarConfig(
                    yearSelection = false,
                    monthSelection = false,
                    style = CalendarStyle.MONTH,
                    disabledTimeline = CalendarTimeline.PAST,
//                    minYear = now.year,
//                    maxYear = now.year + 1
                ),
                selection = CalendarSelection.Date(
                    selectedDate = date,
                    positiveButton = SelectionButton("确认"),
                    negativeButton = SelectionButton("取消"),
                ) { newDate ->
                    date = newDate
                },
            )
        }
    }

    @Composable
    fun ItemChoice(
        label: String = "",
        before: String = "",
        current: String = "",
        after: String = "",
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: String) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
        choice: List<String>,
        choice_label_slice: IntRange
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            Spacer(modifier = Modifier.weight(1f))
            val initial = content.split(" ").asSequence().mapNotNull { it.toIntOrNull() }
                .filter { it in choice.indices }.sorted().toSet()
            val text = initial.joinToString(" ") { choice[it].slice(choice_label_slice) }
            var show by remember {
                mutableStateOf(false)
            }
            TextButton(
                shape = RoundedCornerShape(16.dp),

                onClick = {
                    show = true
                }, enabled = enabled
            ) {
                Text(if (initial.isEmpty()) "无" else text)
            }

            if (show) {
                var value by remember {
                    mutableStateOf(initial)
                }
                value = initial // reinit dialog state on open
                Dialog(
                    onDismissRequest = {
                        show = false
                    }
                ) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, false)
                                    .padding(16.dp)
                            ) {
                                items(choice.indices.toList(), key = { it }) { i ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            choice[i], modifier = Modifier
                                                .weight(1f)
                                        )
                                        Switch(
                                            checked = value.contains(i),
                                            onCheckedChange = {
                                                value = if (it) {
                                                    value.plus(i)
                                                } else {
                                                    value.minus(i)
                                                }
                                            },
                                        )
                                    }
                                }


                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,

                                ) {
                                TextButton(onClick = { show = false }) {
                                    Text("取消")
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                TextButton(onClick = remember {
                                    {
                                        show = false
                                        update {
                                            change(it, value.joinToString(" "))
                                        }
                                    }
                                }) {
                                    Text("确认")
                                }
                            }
                        }

                    }
                }
            }

        }
    }

    @Composable
    fun ItemDrugDay(
        label: String = "",
        before: String = "",
        current: String = "",
        after: String = "",
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: String) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            Spacer(modifier = Modifier.weight(1f))
            val choice = listOf(
                "enable",
                "6.X天药吃几个",
                "5.X天药吃几个",
                "4.X天药吃几个",
                "3.X天药吃几个",
                "2.X天药吃几个",
                "1.X天药吃几个",
                "0.X天药吃几个"
            )
            val initial =
                content.split(" ").mapNotNull { it.toUIntOrNull() }.let { x ->
                    if (x.size > choice.size) {
                        x.slice(choice.indices)
                    } else if (x.size < choice.size) {
                        x + List(choice.size - x.size) { 0u }
                    } else {
                        x
                    }
                }
            var show by remember {
                mutableStateOf(false)
            }
            val text_enable = initial[0] == 1u
            val text = initial.drop(1)
                .joinToString(" ")
            TextButton(
                shape = RoundedCornerShape(16.dp),

                onClick = { show = true }, enabled = enabled
            ) {
                Text(if (text_enable) text else "不自动吃")
            }
            val initial_value = initial.map { it.toString() }

            if (show) {
                var value: List<String> by remember {
                    mutableStateOf(initial_value)
                }
                val enable by remember {
                    derivedStateOf {
                        value[0] == "1"
                    }
                }
                value = initial_value // reinit dialog state on open

                Dialog(
                    onDismissRequest = {
                        show = false
                    }
                ) {
                    Surface(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)

                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, false)
                                    .padding(16.dp)
                            ) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("自动吃临期药", modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = enable,
                                            onCheckedChange = remember {
                                                { new ->
                                                    value = value.toMutableList().apply {
                                                        set(0, if (new) "1" else "0")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                items((1 until choice.size).toList(), key = { it }) { i ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            choice[i], modifier = Modifier
                                                .weight(.5f)
                                                .alpha(
                                                    if (enable)
                                                        1f else 0.5f
                                                )
                                        )
                                        TextField(
                                            enabled = enable,
                                            value = value[i],
                                            onValueChange = {
                                                it.let { new ->
                                                    value = value.toMutableList().apply {
                                                        set(i, new)
                                                    }
                                                }
                                            },
                                            colors = TextFieldDefaults.textFieldColors(
                                                containerColor = Color.Transparent
                                            ),
                                            modifier = Modifier.weight(.5f)
                                        )
                                    }
                                }


                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,

                                ) {
                                TextButton(onClick = { show = false }) {
                                    Text("取消")
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                TextButton(onClick = remember {
                                    {
                                        show = false
                                        update {
                                            change(it, value.joinToString(" "))
                                        }
                                    }
                                }) {
                                    Text("确认")
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun ItemFight(
        label: String = "",
        before: String = "",
        current: String = "",
        after: String = "",
        prefer_before: Boolean = false,
        prefer_after: Boolean = false,
        change: (account: Account, value: String) -> Account = { account, value -> account },
        prefer_change: (account: Account, value: Boolean) -> Account = { account, value -> account },
        disable_prefer: Boolean = false,
        tailingIcon: @Composable (() -> Unit)? = null,
    ) {
        val enabled = enabled(prefer_before, prefer_after)
        if (lite && prefer_before && !prefer_after) return
        val content = content(prefer_before, prefer_after, before, current, after)
        fun valid(text: String): String {
            return text
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Enabled(enabled, prefer_before, prefer_after, prefer_change, disable_prefer)
            Label(enabled, label)
            Spacer(modifier = Modifier.weight(1f))
            var show by remember {
                mutableStateOf(false)
            }
            val initial = content
            TextButton(
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    show = true
                }, enabled = enabled
            ) {
                Text(initial.ifEmpty { "无" }, softWrap = true)
            }

            if (show) {
                var value by remember {
                    mutableStateOf(initial)
                }
                value = initial // reinit dialog state on open
                Dialog(
                    onDismissRequest = {
                        show = false
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(shape = RectangleShape, modifier = Modifier.padding(0.dp)) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                .fillMaxWidth()
                        ) {

//                            var state by remember { mutableStateOf(0) }
                            val titles = listOf("关卡", "材料", "干员")
                            val pagerState = rememberPagerState()
                            TabRow(
                                // Our selected tab is our current page
                                selectedTabIndex = pagerState.currentPage,
                                divider = {

                                }
                            ) {
                                // Add tabs for all of our pages
                                titles.forEachIndexed { index, title ->
                                    Tab(
                                        text = { Text(title) },
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            animationScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                    )
                                }
                            }
                            @Composable
                            fun btn(text: String, append: String? = null, sign: Boolean = false) {
                                OutlinedButton(
                                    shape = RoundedCornerShape(8.dp),
//                                    border = BorderStroke(1.dp, Color.Black),
                                    onClick = {
                                        var space = !sign
                                        val old = value.trim()
                                        val old_end = old.lastOrNull()
                                        if (old_end == null) {
                                            space = false
                                        } else {
                                            if (old_end in listOf(',', '|')) {
                                                space = false
                                            }
                                        }
                                        value =
                                            old + (if (space) " " else "") + (if (append != null) append else text)
                                    }) {
                                    Text(text)
                                }
                            }

                            @Composable
                            fun title(text: String) {
                                Text(
                                    text,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }

                            @Composable
                            fun note(text: String) {
                                Text(
                                    text, style = MaterialTheme.typography.bodyMedium,
                                    color = LocalContentColor.current.copy(alpha = 0.75f)
                                )
                            }
                            HorizontalPager(
                                pageCount = titles.size,
                                state = pagerState,
                                modifier = Modifier.weight(1f, true)
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        LazyColumn(
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .fillMaxSize(),
                                        ) {
                                            item {
                                                title("剿灭")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn("单次剿灭 jm=dqwt|cqwt1|cqwt2|...*1", "jm")
                                                    btn("当期委托 dqwt", "dqwt")
                                                    btn("长期委托1 cqwt1", "cqwt1")
                                                    btn("长期委托2 cqwt2", "cqwt2")
                                                    btn("长期委托3 cqwt3", "cqwt3")
                                                }
                                            }
                                            item {
                                                title("活动")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn("蓝材料及以上活动关 hd=hd-10|hd-9|...*n", "hd")
                                                    btn(
                                                        "活动678平均 hd-6,hd-7,hd-8*n",
                                                        "hd-6,hd-7,hd-8*n"
                                                    )
                                                    btn("hd-10", "hd-10")
                                                    btn("hd-9", "hd-9")
                                                    btn("hd-8")
                                                    btn("hd-7")
                                                    btn("hd-6")
                                                    btn("hd-5")
                                                    btn("hd-4")
                                                    btn("hd-3")
                                                    btn("hd-2")
                                                    btn("hd-1")
                                                }
                                            }
                                            item {
                                                title("物资")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn("龙门币 ce=ce-6|ce-5|...|ce-1*n", "ce")
                                                    btn("作战记录 ls=ls-6|ls-5|...|ls-1*n", "ls")
                                                    btn("采购凭证 ap=ap-6|ap-5|...|ap-1*n", "ap")
                                                    btn("技巧概要 ca=ca-6|ca-5|...|ca-1*n", "ca")
                                                    btn("碳 sk=sk-6|sk-5|...|sk-1*n", "sk")

                                                }
                                            }
                                            item {
                                                title("芯片")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn(
                                                        "芯片平均 pr=pr-b-2,pr-a-2,pr-c-2,pr-d-2*n pr-b-1,pr-a-1,pr-c-1,pr-d-1*n",
                                                        "pr"
                                                    )
                                                    btn("近卫特种芯片 prd=pr-d-2*n pr-d-1*n", "prd")
                                                    btn("先锋辅助芯片 prc=pr-c-2*n pr-c-1*n", "prc")
                                                    btn("狙击术师芯片 prb=pr-b-2*n pr-b-1*n", "prb")
                                                    btn("重装医疗芯片 pra=pr-a-2*n pr-a-1*n", "pra")
                                                }
                                            }
                                            item {
                                                title("主线")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn("11-8(普通或磨难)", "11-8")
                                                    btn("r8-11")
                                                    btn("jt8-3")
                                                    btn("1-7")
                                                    // TODO 所有支持关卡
//                                                    note("等等")
                                                }
                                            }
                                            item {
                                                title("上一次")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn("上一次 syc", "syc")
                                                }
                                            }
                                            item {
                                                title("组合")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn(
                                                        "首次成功后跳过 |",
                                                        "|", true
                                                    )
                                                    btn(
                                                        "依次执行 ,", ",", true
                                                    )

                                                }
                                            }
                                            item {
                                                title("重复")
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    note("单关卡或关卡组合末尾使用，组合中间不能使用")

                                                    btn(
                                                        "重复4次 *4", "*4", true
                                                    )
                                                    btn(
                                                        "重复无限次 *n", "*n", true
                                                    )

                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        LazyColumn(
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .fillMaxSize()
                                        ) {
                                            // TODO 前后加关卡按钮, 数据从
                                            item {
                                                Text(
                                                    "材料："
                                                )
                                                Text(
                                                    "TODO"
                                                )
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn(
                                                        "至少4个 >=4", ">=4", true
                                                    )
                                                    btn(
                                                        "无限个 >=n", ">=n", true
                                                    )
                                                    btn(
                                                        "满足即删 #", "#", true
                                                    )
                                                    btn(
                                                        "自动合成 @", "@", true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        LazyColumn(
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .fillMaxSize()
                                        ) {
                                            // TODO 前后加关卡按钮, 数据从
                                            item {
                                                Text(
                                                    "干员："
                                                )
                                                Text(
                                                    "TODO"
                                                )
                                            }
                                            item {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    btn(
                                                        "能天使"
                                                    )
                                                    btn(
                                                        "精[012] [0-90]级"
                                                    )
                                                    btn(
                                                        "[123]技能专[123]"
                                                    )
                                                    btn(
                                                        "满足即删 #"
                                                    )
                                                    btn(
                                                        "自动合成升级 @"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextField(
                                    modifier = Modifier.weight(1f),
                                    value = value,
                                    onValueChange = {
                                        value = it
                                    },
                                    colors = TextFieldDefaults.textFieldColors(
                                        containerColor = Color.Transparent
                                    ),
                                )
                                TextButton(onClick = { show = false }) {
                                    Text("取消")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                TextButton(onClick = remember {
                                    {
                                        show = false
                                        update {
                                            change(it, valid(value))
                                        }
                                    }
                                }) {
                                    Text("确认")
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun username() = Item(
        label = "账号",
        current = account.username,
        change = { account, value ->
            account.copy(username = value)
        },
        disable_prefer = true,
    ) {
        TextButton(onClick = remember {
            {
                update {
                    it.copy(server = if (it.server == "B服") "官服" else "B服")
                }
            }
        }) {
            Text(if (account.server == "B服") "B服" else "官服")
        }
    }

    @Composable
    fun password() = Item(
        label = "密码",
        current = account.password,
        change = { account, value ->
            account.copy(password = value)
        },
        disable_prefer = true
    ) {
        TextButton(
            enabled = !account.evaluating,
            onClick = remember {
                {
                    update {
                        it.copy(evaluating = true)
                    }
                }
            }) {

            if (account.evaluating) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(.5f)
                        .padding(end = 2.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                "校验",
            )
        }
    }

    @Composable
    fun note() = Item(
        label = "备注",
        current = account.note,
        change = { account, value ->
            account.copy(note = value)
        },
        disable_prefer = true
    )

    @Composable
    fun priority() = Item(
        label = "优先等级",
        before = before.priority,
        current = account.priority,
        after = after.priority,
        prefer_before = !account.priority_override,
        prefer_after = after.priority_override,
        change = { account, value ->
            account.copy(priority = value)
        },
        prefer_change = { account, value ->
            account.copy(priority_override = value)
        },
    )

    @Composable
    fun allow_begin_date() = ItemDate(
        label = "允许日期",
        before = before.allow_begin_date,
        current = account.allow_begin_date,
        after = after.allow_begin_date,
        prefer_before = !account.allow_begin_date_override,
        prefer_after = after.allow_begin_date_override,
        change = { account, value ->
            account.copy(allow_begin_date = value)
        },
        prefer_change = { account, value ->
            account.copy(allow_begin_date_override = value)
        },
    )

    @Composable
    fun forbid_weekday() = ItemChoice(
        label = "禁用星期",
        before = before.forbid_weekday,
        current = account.forbid_weekday,
        after = after.forbid_weekday,
        prefer_before = !account.forbid_weekday_override,
        prefer_after = after.forbid_weekday_override,
        change = { account, value ->
            account.copy(forbid_weekday = value)
        },
        prefer_change = { account, value ->
            account.copy(forbid_weekday_override = value)
        },
        choice = listOf(
            "周一不打",
            "周二不打",
            "周三不打",
            "周三不打",
            "周四不打",
            "周五不打",
            "周六不打",
            "周日不打"
        ),
        choice_label_slice = 1..1,
    )

    @Composable
    fun crontab_start() =
        Item(
            label = "首次时间",
            before = before.crontab_start,
            current = account.crontab_start,
            after = after.crontab_start,
            prefer_before = !account.crontab_start_override,
            prefer_after = after.crontab_start_override,
            change = { account, value ->
                account.copy(crontab_start = value)
            },
            prefer_change = { account, value ->
                account.copy(crontab_start_override = value)
            },
        )

    @Composable
    fun crontab_step() = Item(
        label = "间隔小时",
        before = before.crontab_step,
        current = account.crontab_step,
        after = after.crontab_step,
        prefer_before = !account.crontab_step_override,
        prefer_after = after.crontab_step_override,
        change = { account, value ->
            account.copy(crontab_step = value)
        },
        prefer_change = { account, value ->
            account.copy(crontab_step_override = value)
        },
    )

    @Composable
    fun job_mail() = Item(
        label = "邮件",
        before = before.job_mail,
        current = account.job_mail,
        after = after.job_mail,
        prefer_before = !account.job_mail_override,
        prefer_after = after.job_mail_override,
        change = { account, value ->
            account.copy(job_mail = value)
        },
        prefer_change = { account, value ->
            account.copy(job_mail_override = value)
        },
    )

    @Composable
    fun job_fight() = Item(
        label = "作战",
        before = before.job_fight,
        current = account.job_fight,
        after = after.job_fight,
        prefer_before = !account.job_fight_override,
        prefer_after = after.job_fight_override,
        change = { account, value ->
            account.copy(job_fight = value)
        },
        prefer_change = { account, value ->
            account.copy(job_fight_override = value)
        },
    )

    @Composable
    fun fight() = ItemFight(
        label = "作战目标",
        before = before.fight,
        current = account.fight,
        after = after.fight,
        prefer_before = !account.fight_override,
        prefer_after = after.fight_override,
        change = { account, value ->
            account.copy(fight = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_override = value)
        },
    )

    @Composable
    fun max_stone() = Item(
        label = "吃石次数",
        before = before.max_stone,
        current = account.max_stone,
        after = after.max_stone,
        prefer_before = !account.max_stone_override,
        prefer_after = after.max_stone_override,
        change = { account, value ->
            account.copy(max_stone = value)
        },
        prefer_change = { account, value ->
            account.copy(max_stone_override = value)
        },
    )

    @Composable
    fun max_drug() = Item(
        label = "吃药次数",
        before = before.max_drug,
        current = account.max_drug,
        after = after.max_drug,
        prefer_before = !account.max_drug_override,
        prefer_after = after.max_drug_override,
        change = { account, value ->
            account.copy(max_drug = value)
        },
        prefer_change = { account, value ->
            account.copy(max_drug_override = value)
        },
    )

    @Composable
    fun max_drug_day() = ItemDrugDay(
        label = "吃临期药",
        before = before.max_drug_day,
        current = account.max_drug_day,
        after = after.max_drug_day,
        prefer_before = !account.max_drug_day_override,
        prefer_after = after.max_drug_day_override,
        change = { account, value ->
            account.copy(max_drug_day = value)
        },
        prefer_change = { account, value ->
            account.copy(max_drug_day_override = value)
        },
    )


// TODO 做成多项选择
    // @Composable
    // fun fight_activity_shop() = Item(
    //     label = "活动相关",
    //     before = before.fight_activity_shop,
    //     current = account.fight_activity_shop,
    //     after = after.fight_activity_shop,
    //     prefer_before = !account.fight_activity_shop_override,
    //     prefer_after = after.fight_activity_shop_override,
    //     change = { account, value ->
    //         account.copy(fight_activity_shop = value)
    //     },
    //     prefer_change = { account, value ->
    //         account.copy(fight_activity_shop_override = value)
    //     },
    // )

    @Composable
    fun job_dorm() = Item(
        label = "基建",
        before = before.job_dorm,
        current = account.job_dorm,
        after = after.job_dorm,
        prefer_before = !account.job_dorm_override,
        prefer_after = after.job_dorm_override,
        change = { account, value ->
            account.copy(job_dorm = value)
        },
        prefer_change = { account, value ->
            account.copy(job_dorm_override = value)
        },
    )

    @Composable
    fun job_dorm_item() = ItemChoice(
        label = "基建事项",
        before = before.job_dorm_item,
        current = account.job_dorm_item,
        after = after.job_dorm_item,
        prefer_before = !account.job_dorm_item_override,
        prefer_after = after.job_dorm_item_override,
        change = { account, value ->
            account.copy(job_dorm_item = value)
        },
        prefer_change = { account, value ->
            account.copy(job_dorm_item_override = value)
        },
        choice = listOf("访友", "收获", "排班", "加速", "副手", "线索"),
        choice_label_slice = 0..1,
    )

    @Composable
    fun give_away_all_clue() = Item(
        label = "线索全送",
        before = before.give_away_all_clue,
        current = account.give_away_all_clue,
        after = after.give_away_all_clue,
        prefer_before = !account.give_away_all_clue_override,
        prefer_after = after.give_away_all_clue_override,
        change = { account, value ->
            account.copy(give_away_all_clue = value)
        },
        prefer_change = { account, value ->
            account.copy(give_away_all_clue_override = value)
        },
    )

    @Composable
    fun job_shop() = Item(
        label = "信交",
        before = before.job_shop,
        current = account.job_shop,
        after = after.job_shop,
        prefer_before = !account.job_shop_override,
        prefer_after = after.job_shop_override,
        change = { account, value ->
            account.copy(job_shop = value)
        },
        prefer_change = { account, value ->
            account.copy(job_shop_override = value)
        },
    )

    @Composable
    fun prefer_goods() = Item(
        label = "先买",
        before = before.prefer_goods,
        current = account.prefer_goods,
        after = after.prefer_goods,
        prefer_before = !account.prefer_goods_override,
        prefer_after = after.prefer_goods_override,
        change = { account, value ->
            account.copy(prefer_goods = value)
        },
        prefer_change = { account, value ->
            account.copy(prefer_goods_override = value)
        },
    )

    @Composable
    fun forbid_goods() = Item(
        label = "不买",
        before = before.forbid_goods,
        current = account.forbid_goods,
        after = after.forbid_goods,
        prefer_before = !account.forbid_goods_override,
        prefer_after = after.forbid_goods_override,
        change = { account, value ->
            account.copy(forbid_goods = value)
        },
        prefer_change = { account, value ->
            account.copy(forbid_goods_override = value)
        },
    )

    @Composable
    fun job_recruit() = Item(
        label = "公招",
        before = before.job_recruit,
        current = account.job_recruit,
        after = after.job_recruit,
        prefer_before = !account.job_recruit_override,
        prefer_after = after.job_recruit_override,
        change = { account, value ->
            account.copy(job_recruit = value)
        },
        prefer_change = { account, value ->
            account.copy(job_recruit_override = value)
        },
    )

    @Composable
    fun auto_recruit() = ItemChoice(
        label = "自动招募",
        before = before.auto_recruit,
        current = account.auto_recruit,
        after = after.auto_recruit,
        prefer_before = !account.auto_recruit_override,
        prefer_after = after.auto_recruit_override,
        change = { account, value ->
            account.copy(auto_recruit = value)
        },
        prefer_change = { account, value ->
            account.copy(auto_recruit_override = value)
        },
        choice = listOf("保底六星", "保底五星", "保底四星", "保底小车", "\u200B\u200B普通"),
        choice_label_slice = 2..3,
    )

    @Composable
    fun job_task() = Item(
        label = "任务",
        before = before.job_task,
        current = account.job_task,
        after = after.job_task,
        prefer_before = !account.job_task_override,
        prefer_after = after.job_task_override,
        change = { account, value ->
            account.copy(job_task = value)
        },
        prefer_change = { account, value ->
            account.copy(job_task_override = value)
        },
    )

    @Composable
    fun job_activity_checkin() = Item(
        label = "签到抽签洽谈",
        before = before.job_activity_checkin,
        current = account.job_activity_checkin,
        after = after.job_activity_checkin,
        prefer_before = !account.job_activity_checkin_override,
        prefer_after = after.job_activity_checkin_override,
        change = { account, value ->
            account.copy(job_activity_checkin = value)
        },
        prefer_change = { account, value ->
            account.copy(job_activity_checkin_override = value)
        },
    )

    @Composable
    fun job_activity_recruit() = Item(
        label = "赠送寻访",
        before = before.job_activity_recruit,
        current = account.job_activity_recruit,
        after = after.job_activity_recruit,
        prefer_before = !account.job_activity_recruit_override,
        prefer_after = after.job_activity_recruit_override,
        change = { account, value ->
            account.copy(job_activity_recruit = value)
        },
        prefer_change = { account, value ->
            account.copy(job_activity_recruit_override = value)
        },
    )

    @Composable
    fun fight_max_failed_times() = Item(
        label = "关卡连续失败几次后跳过",
        before = before.fight_max_failed_times,
        current = account.fight_max_failed_times,
        after = after.fight_max_failed_times,
        prefer_before = !account.fight_max_failed_times_override,
        prefer_after = after.fight_max_failed_times_override,
        change = { account, value ->
            account.copy(fight_max_failed_times = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_max_failed_times_override = value)
        },
    )

    @Composable
    fun login_max_see_times() = Item(
        label = "账号15分钟第几次登录前跳过",
        before = before.login_max_see_times,
        current = account.login_max_see_times,
        after = after.login_max_see_times,
        prefer_before = !account.login_max_see_times_override,
        prefer_after = after.login_max_see_times_override,
        change = { account, value ->
            account.copy(login_max_see_times = value)
        },
        prefer_change = { account, value ->
            account.copy(login_max_see_times_override = value)
        },
    )

    @Composable
    fun captcha_username() = Item(
        label = "图鉴账号",
        before = before.captcha_username,
        current = account.captcha_username,
        after = after.captcha_username,
        prefer_before = !account.captcha_username_override,
        prefer_after = after.captcha_username_override,
        change = { account, value ->
            account.copy(captcha_username = value)
        },
        prefer_change = { account, value ->
            account.copy(captcha_username_override = value)
        },
    )

    @Composable
    fun captcha_password() = Item(
        label = "图鉴密码",
        before = before.captcha_password,
        current = account.captcha_password,
        after = after.captcha_password,
        prefer_before = !account.captcha_password_override,
        prefer_after = after.captcha_password_override,
        change = { account, value ->
            account.copy(captcha_password = value)
        },
        prefer_change = { account, value ->
            account.copy(captcha_password_override = value)
        },
    )

    @Composable
    fun fight_pass() = ItemFight(
        label = "过图(TODO)",
        before = before.fight_pass,
        current = account.fight_pass,
        after = after.fight_pass,
        prefer_before = !account.fight_pass_override,
        prefer_after = after.fight_pass_override,
        change = { account, value ->
            account.copy(fight_pass = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_pass_override = value)
        },
    )

    @Composable
    fun 傀影() = Item(
        label = "傀影",
        before = before.fight_pass,
        current = account.fight_pass,
        after = after.fight_pass,
        prefer_before = !account.fight_pass_override,
        prefer_after = after.fight_pass_override,
        change = { account, value ->
            account.copy(fight_pass = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_pass_override = value)
        },
    )

    @Composable
    fun 水月() = Item(
        label = "水月",
        before = before.fight_pass,
        current = account.fight_pass,
        after = after.fight_pass,
        prefer_before = !account.fight_pass_override,
        prefer_after = after.fight_pass_override,
        change = { account, value ->
            account.copy(fight_pass = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_pass_override = value)
        },
    )

    @Composable
    fun 保全() = Item(
        label = "保全",
        before = before.fight_pass,
        current = account.fight_pass,
        after = after.fight_pass,
        prefer_before = !account.fight_pass_override,
        prefer_after = after.fight_pass_override,
        change = { account, value ->
            account.copy(fight_pass = value)
        },
        prefer_change = { account, value ->
            account.copy(fight_pass_override = value)
        },
    )

    val see_job_fight = see(
        !account.job_fight_override,
        after.job_fight_override,
        before.job_fight,
        account.job_fight,
        after.job_fight
    )
    val see_job_dorm = see(
        !account.job_dorm_override,
        after.job_dorm_override,
        before.job_dorm,
        account.job_dorm,
        after.job_dorm
    )
    val see_job_shop = see(
        !account.job_shop_override,
        after.job_shop_override,
        before.job_shop,
        account.job_shop,
        after.job_shop
    )
    val see_job_recruit = see(
        !account.job_recruit_override,
        after.job_recruit_override,
        before.job_recruit,
        account.job_recruit,
        after.job_recruit
    )

    if (lite) {
        priority()
        allow_begin_date()
        forbid_weekday()
        crontab_start()
        crontab_step()
        job_mail()

        job_fight()
        if (see_job_fight) {
            fight()
            max_stone()
            max_drug()
            max_drug_day()
            // fight_activity_shop()
        }

        job_dorm()
        if (see_job_dorm) {
            job_dorm_item()
            give_away_all_clue()
        }

        job_shop()
        if (see_job_shop) {
            prefer_goods()
            forbid_goods()
        }

        job_recruit()
        if (see_job_recruit) {
            auto_recruit()
        }

        job_task()
        job_activity_checkin()
        job_activity_recruit()

        fight_pass()
        // 傀影()
        // 水月()
        // 保全()

        fight_max_failed_times()
        login_max_see_times()
        captcha_username()
        captcha_password()

    } else {
        LazyColumn(
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .padding(start = 0.dp, end = 16.dp)
        ) {
            if (!before_account && !after_account) {
                item {
                    username()
                }
                item {
                    password()
                }
                item {
                    note()
                }
            }
            item {
                Section(
                    "时间",
                )
            }
            item {
                priority()
            }
            item {
                allow_begin_date()

            }
            item {
                forbid_weekday()
            }
            item {
                crontab_start()
            }


            item {
                crontab_step()
            }


            item {
                Section(
                    "日常",
                )
            }
            item {
                job_mail()
            }
            item {
                job_fight()
            }

            if (see_job_fight) {
                item {
                    fight()
                }
                item {
                    max_stone()
                }
                item {
                    max_drug()
                }
                item {
                    max_drug_day()
                }
                item {
                    // fight_activity_shop()
                }
            }


            item {
                job_dorm()
            }

            if (see_job_dorm) {

                item {
                    job_dorm_item()
                }
                item {
                    give_away_all_clue()
                }
            }

            item {
                job_shop()
            }

            if (see_job_shop) {
                item {
                    prefer_goods()
                }

                item {
                    forbid_goods()
                }
            }

            item {
                job_recruit()
            }

            if (see_job_recruit) {
                item {
                    auto_recruit()
                }
            }

            item {
                job_task()
            }
            item {
                job_activity_checkin()
            }
            item {
                job_activity_recruit()
            }

            item {
                Section(
                    "非日常",
                )
            }
            item {
                fight_pass()
            }

            item {
                Section(
                    "容错",
                )
            }

            item {
                fight_max_failed_times()
            }
            item {
                login_max_see_times()
            }

            item {
                captcha_username()
            }
            item {
                captcha_password()
            }
        }
    }
}
