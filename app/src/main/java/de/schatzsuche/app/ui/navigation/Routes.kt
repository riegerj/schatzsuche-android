package de.schatzsuche.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val ADMIN = "admin"
    const val ADMIN_HUNT_EDIT = "admin/hunt/{huntId}"
    const val ADMIN_STEP_EDIT = "admin/hunt/{huntId}/step/{stepId}"
    const val ADMIN_QR_HELP = "admin/hunt/{huntId}/qr_help"
    const val ADMIN_SESSIONS = "admin/sessions"
    const val ADMIN_SESSION_DETAIL = "admin/session/{sessionId}"
    const val ADMIN_QR_PDF = "admin/qr_pdf"
    const val PARTICIPANT = "participant"
    const val PARTICIPANT_START = "participant/start/{huntId}"
    const val PLAY = "play/{sessionId}"
    const val SUMMARY = "summary/{sessionId}"

    fun huntEdit(huntId: String) = "admin/hunt/$huntId"
    fun stepEdit(huntId: String, stepId: String) = "admin/hunt/$huntId/step/$stepId"
    fun qrHelp(huntId: String) = "admin/hunt/$huntId/qr_help"
    fun participantStart(huntId: String) = "participant/start/$huntId"
    fun play(sessionId: String) = "play/$sessionId"
    fun summary(sessionId: String) = "summary/$sessionId"
    fun sessionDetail(sessionId: String) = "admin/session/$sessionId"
}
