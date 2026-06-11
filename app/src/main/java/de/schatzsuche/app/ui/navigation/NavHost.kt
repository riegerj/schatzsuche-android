package de.schatzsuche.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.schatzsuche.app.data.repository.SchatzsucheRepository
import de.schatzsuche.app.ui.screens.admin.AdminHomeScreen
import de.schatzsuche.app.ui.screens.admin.HuntEditScreen
import de.schatzsuche.app.ui.screens.admin.QrHelpScreen
import de.schatzsuche.app.ui.screens.admin.QrPdfScreen
import de.schatzsuche.app.ui.screens.admin.SessionDetailScreen
import de.schatzsuche.app.ui.screens.admin.SessionsOverviewScreen
import de.schatzsuche.app.ui.screens.admin.StepEditScreen
import de.schatzsuche.app.ui.screens.home.HomeScreen
import de.schatzsuche.app.ui.screens.participant.ParticipantHomeScreen
import de.schatzsuche.app.ui.screens.participant.ParticipantStartScreen
import de.schatzsuche.app.ui.screens.play.PlayScreen
import de.schatzsuche.app.ui.screens.setup.InitialSetupScreen
import de.schatzsuche.app.ui.screens.summary.SummaryScreen
import de.schatzsuche.app.ui.viewmodel.AdminViewModel
import de.schatzsuche.app.ui.viewmodel.AdminViewModelFactory
import de.schatzsuche.app.ui.viewmodel.ParticipantViewModel
import de.schatzsuche.app.ui.viewmodel.ParticipantViewModelFactory
import de.schatzsuche.app.ui.viewmodel.PlayViewModel
import de.schatzsuche.app.ui.viewmodel.PlayViewModelFactory
import de.schatzsuche.app.ui.viewmodel.SetupViewModel
import de.schatzsuche.app.ui.viewmodel.SetupViewModelFactory
import de.schatzsuche.app.ui.viewmodel.SummaryViewModel
import de.schatzsuche.app.ui.viewmodel.SummaryViewModelFactory

@Composable
fun SchatzsucheNavHost(repository: SchatzsucheRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onAdmin = { navController.navigate(Routes.ADMIN) },
                onParticipant = { navController.navigate(Routes.PARTICIPANT) },
                onSetup = { navController.navigate(Routes.SETUP) }
            )
        }

        composable(Routes.SETUP) {
            val vm: SetupViewModel = viewModel(factory = SetupViewModelFactory(repository))
            InitialSetupScreen(
                viewModel = vm,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN) {
            val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(repository))
            AdminHomeScreen(
                viewModel = vm,
                onEditHunt = { navController.navigate(Routes.huntEdit(it)) },
                onSessions = { navController.navigate(Routes.ADMIN_SESSIONS) },
                onQrPdf = { navController.navigate(Routes.ADMIN_QR_PDF) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADMIN_HUNT_EDIT,
            arguments = listOf(navArgument("huntId") { type = NavType.StringType })
        ) { entry ->
            val huntId = entry.arguments?.getString("huntId") ?: return@composable
            val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(repository))
            HuntEditScreen(
                viewModel = vm,
                huntId = huntId,
                onEditStep = { hId, sId -> navController.navigate(Routes.stepEdit(hId, sId)) },
                onQrHelp = { navController.navigate(Routes.qrHelp(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADMIN_STEP_EDIT,
            arguments = listOf(
                navArgument("huntId") { type = NavType.StringType },
                navArgument("stepId") { type = NavType.StringType }
            )
        ) { entry ->
            val huntId = entry.arguments?.getString("huntId") ?: return@composable
            val stepId = entry.arguments?.getString("stepId") ?: return@composable
            val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(repository))
            StepEditScreen(
                viewModel = vm,
                huntId = huntId,
                stepId = stepId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADMIN_QR_HELP,
            arguments = listOf(navArgument("huntId") { type = NavType.StringType })
        ) { entry ->
            val huntId = entry.arguments?.getString("huntId") ?: return@composable
            val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(repository))
            QrHelpScreen(viewModel = vm, huntId = huntId, onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_SESSIONS) {
            val vm: AdminViewModel = viewModel(factory = AdminViewModelFactory(repository))
            SessionsOverviewScreen(
                viewModel = vm,
                onSessionClick = { navController.navigate(Routes.sessionDetail(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADMIN_SESSION_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { entry ->
            val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
            SessionDetailScreen(
                repository = repository,
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_QR_PDF) {
            val vm: SetupViewModel = viewModel(factory = SetupViewModelFactory(repository))
            QrPdfScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.PARTICIPANT) {
            val vm: ParticipantViewModel = viewModel(factory = ParticipantViewModelFactory(repository))
            ParticipantHomeScreen(
                viewModel = vm,
                onStartHunt = { navController.navigate(Routes.participantStart(it)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.PARTICIPANT_START,
            arguments = listOf(navArgument("huntId") { type = NavType.StringType })
        ) { entry ->
            val huntId = entry.arguments?.getString("huntId") ?: return@composable
            val vm: ParticipantViewModel = viewModel(factory = ParticipantViewModelFactory(repository))
            ParticipantStartScreen(
                viewModel = vm,
                huntId = huntId,
                onStarted = { sessionId ->
                    navController.navigate(Routes.play(sessionId)) {
                        popUpTo(Routes.PARTICIPANT) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.PLAY,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { entry ->
            val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
            val vm: PlayViewModel = viewModel(factory = PlayViewModelFactory(repository, sessionId))
            val session by vm.session.collectAsState()
            PlayScreen(
                viewModel = vm,
                onFinished = { navController.navigate(Routes.summary(it)) { popUpTo(Routes.HOME) } },
                onCancelled = { navController.navigate(Routes.PARTICIPANT) { popUpTo(Routes.HOME) } }
            )
        }

        composable(
            Routes.SUMMARY,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { entry ->
            val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
            val vm: SummaryViewModel = viewModel(factory = SummaryViewModelFactory(repository, sessionId))
            SummaryScreen(
                viewModel = vm,
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}
