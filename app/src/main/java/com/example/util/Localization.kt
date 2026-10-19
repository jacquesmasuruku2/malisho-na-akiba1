package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.UserRole

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val subtitle: String,
    val flagEmoji: String
) {
    FRENCH("fr", "Français", "Langue officielle de travail", "🇫🇷"),
    SWAHILI("sw", "Kiswahili (RDC)", "Kiswahili chepesi cha masoko na biashara", "🇨🇩")
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FRENCH }

@Composable
fun tr(key: String): String {
    return Localization.tr(key, LocalAppLanguage.current)
}

object Localization {

    fun tr(key: String, lang: AppLanguage): String {
        return if (lang == AppLanguage.SWAHILI) {
            swahiliStrings[key]
                ?: swahiliPhraseMap[key]
                ?: frenchStrings[key]
                ?: key
        } else {
            frenchStrings[key] ?: key
        }
    }

    fun roleLabel(role: UserRole?, lang: AppLanguage): String {
        if (role == null) return ""
        return when (role) {
            UserRole.GESTIONNAIRE -> if (lang == AppLanguage.SWAHILI) "Usimamizi Mkuu" else "Direction Générale"
            UserRole.AGENT -> if (lang == AppLanguage.SWAHILI) "Afisa wa Soko" else "Agent de Terrain"
            UserRole.CAISSIER -> if (lang == AppLanguage.SWAHILI) "Mweka Hazina (Kechi)" else "Caissier Central"
            UserRole.CLIENT -> if (lang == AppLanguage.SWAHILI) "Mteja" else "Client"
        }
    }

    fun statusLabel(status: String, lang: AppLanguage): String {
        val s = status.uppercase()
        return if (lang == AppLanguage.SWAHILI) {
            when {
                s.contains("RETARD") -> "Imechelewa"
                s.contains("DEFAUT") -> "Imeharibika"
                s.contains("ACTIF") || s.contains("COURS") || s.contains("DECAISSE") -> "Inaendelea"
                s.contains("CLOTURE") || s.contains("REMBOURSE") || s.contains("PAYE") || s.contains("TERMINE") -> "Imemalizika"
                s.contains("APPROUVE") -> "Imekubaliwa"
                s.contains("ATTENTE") || s.contains("SOUMISE") || s.contains("ETUDE") || s.contains("PREVU") -> "Inangojea"
                s.contains("REJETE") || s.contains("REFUSE") || s.contains("ANNULE") -> "Imekataliwa"
                s.contains("BROUILLON") -> "Bado haijatumwa"
                else -> status
            }
        } else {
            when {
                s.contains("RETARD") -> "En retard"
                s.contains("DEFAUT") -> "En défaut"
                s.contains("ACTIF") || s.contains("COURS") || s.contains("DECAISSE") -> "En cours"
                s.contains("CLOTURE") || s.contains("REMBOURSE") || s.contains("PAYE") || s.contains("TERMINE") -> "Soldé"
                s.contains("APPROUVE") -> "Approuvé"
                s.contains("ATTENTE") || s.contains("SOUMISE") || s.contains("ETUDE") || s.contains("PREVU") -> "En attente"
                s.contains("REJETE") || s.contains("REFUSE") || s.contains("ANNULE") -> "Rejeté"
                s.contains("BROUILLON") -> "Brouillon"
                else -> status
            }
        }
    }

    // Direct phrase translation for UI labels that might be passed directly
    private val swahiliPhraseMap = mapOf(
        "Tous" to "Wote",
        "Toutes" to "Zote",
        "Actifs" to "Wazuri",
        "En cours" to "Inayoendelea",
        "En retard" to "Iliyochelewa",
        "Terminés" to "Zilizoisha",
        "Entrées" to "Zilizoingia",
        "Sorties" to "Zilizotoka",
        "Mes clients" to "Wateja wangu",
        "Encaisser" to "Kukusanya",
        "Relancer" to "Kukumbusha",
        "Détails" to "Habari Zaidi",
        "Dossier" to "Faili",
        "Journalier" to "Kila Siku",
        "Hebdomadaire" to "Kila Juma",
        "Mensuel" to "Kila Mwezi",
        "Retour" to "Kurudi",
        "Fermer" to "Kufunga",
        "Annuler" to "Acha",
        "Confirmer" to "Kukubali",
        "Enregistrer" to "Kuchunga",
        "Supprimer" to "Kufuta",
        "Modifier" to "Kugeuza",
        "Direction" to "Usimamizi",
        "Agent Terrain" to "Afisa wa Soko",
        "Client" to "Mteja",
        "Caissier" to "Mweka Hazina",
        "Utilisateur" to "Mtumiaji",
        "Solde restant" to "Deni Inayobaki",
        "Échéance/jour" to "Malipo ya Siku",
        "Retard constaté" to "Kuchelewa kwa Malipo"
    )

    private val frenchStrings = mapOf(
        // App Identity
        "app_title" to "MALISHO NA AKIBA",
        "app_acronym" to "\"MALAKI\"",
        "app_tagline" to "Système Opérationnel de Microcrédits & Épargne\nMarchés de Goma • Recouvrement Journalier 3,5%",

        // Navigation
        "nav_dashboard" to "Tableau",
        "nav_clients" to "Clients",
        "nav_cycles" to "Crédits",
        "nav_caisse" to "Caisse",
        "nav_reports" to "Rapports",
        "nav_settings" to "Paramètres",
        "nav_collect" to "Encaisser",
        "nav_chat" to "Chat",
        "nav_apply" to "Demander",
        "nav_schedule" to "Échéances",
        "nav_advisor" to "Chat Conseiller",
        "nav_home" to "Accueil",

        // Screen Titles
        "title_dashboard" to "Tableau de Bord Général",
        "title_clients" to "Gestion des Clients",
        "title_client_detail" to "Détail Client",
        "title_cycles" to "Cycles de Crédit",
        "title_cycle_detail" to "Détail du Prêt",
        "title_applications" to "Demandes de Prêt",
        "title_record_payment" to "Encaissement Recouvrement",
        "title_caisse" to "Gestion de la Caisse",
        "title_reports" to "Rapports d'Activité",
        "title_client_portal" to "Espace Client",
        "title_client_schedule" to "Mon Échéancier",
        "title_client_loan_apply" to "Nouvelle Demande de Prêt",
        "title_chat" to "Chat",
        "title_notifications" to "Centre de Notifications",
        "title_user_management" to "Gestion du Personnel",
        "title_projections" to "Projection sur 6 Cycles",
        "title_settings" to "Paramètres & Préférences",
        "title_audit" to "Audit & Sécurité",
        "title_more" to "Administration & Outils",
        "title_agent_home" to "Accueil Terrain",

        // Settings
        "settings_language_section" to "CHOIX DE LA LANGUE D'AFFICHAGE",
        "settings_language_desc" to "Basculez entre le Français et le Kiswahili facile de la RDC (utilisé couramment sur les marchés de Goma et l'Est) :",
        "settings_lang_fr" to "Français",
        "settings_lang_sw" to "Kiswahili (RDC)",
        "settings_lang_active" to "Langue active",
        "settings_theme_section" to "THÈME D'AFFICHAGE (LISIBILITÉ)",
        "settings_theme_desc" to "Choisissez votre mode d'affichage pour une lisibilité optimale sur les marchés et au bureau :",
        "settings_theme_light" to "Mode Clair",
        "settings_theme_dark" to "Mode Sombre",
        "settings_theme_auto" to "Système",
        "settings_google_section" to "COMPTE GOOGLE ACTIF",
        "settings_google_logout" to "Se déconnecter de Google",
        "settings_business_section" to "IDENTITÉ DE L'INSTITUTION",
        "settings_org_name" to "Nom de l'organisation",
        "settings_initial_cap" to "Capital initial (FC)",
        "settings_target_cap" to "Capital cible (FC)",
        "settings_rates_section" to "BARÈMES CRÉDIT PAR DÉFAUT",
        "settings_daily_rate" to "Taux quotidien (%)",
        "settings_duration_days" to "Durée cycle (jours)",
        "settings_profit_split" to "RÉPARTITION DU PROFIT & PROVISIONS",
        "settings_share_company" to "Part Entreprise (%)",
        "settings_share_client" to "Part Client (%)",
        "settings_expense_rate" to "Charges d'exploit. (%)",
        "settings_loss_rate" to "Prov. pour pertes (%)",
        "settings_save" to "Enregistrer les Paramètres",
        "settings_modules_section" to "AUTRES MODULES D'ADMINISTRATION",
        "settings_saved_success" to "Paramètres enregistrés avec succès",

        // Roles & Auth
        "role_manager" to "Direction Générale",
        "role_agent" to "Agent de Terrain",
        "role_cashier" to "Caissier Central",
        "role_client" to "Client",
        "auth_choose_account" to "Choisir un compte",
        "auth_continue_to" to "pour continuer vers MALISHO NA AKIBA (MALAKI)",
        "auth_signin_google" to "Continuer avec Google",
        "auth_or_credentials" to "OU SE CONNECTER AVEC IDENTIFIANTS",
        "auth_login" to "Se Connecter",
        "auth_username" to "Nom d'utilisateur",
        "auth_pin" to "Code PIN (4 chiffres)",
        "auth_logout" to "Se Déconnecter",
        "auth_biometric" to "Connexion par Empreinte Digitale",
        "auth_biometric_locked" to "Application Verrouillée par Empreinte",
        "auth_biometric_success" to "Empreinte digitale reconnue avec succès",
        "auth_biometric_fail" to "Empreinte digitale non reconnue",
        "auth_biometric_pin_fallback" to "Utiliser le code PIN",

        // Top Bar & RBAC Dialog
        "topbar_role_title" to "Espace Gestionnaire : Rôles & Accès",
        "topbar_google_account" to "Compte Google authentifié :",
        "topbar_switch_role_desc" to "Basculez entre les rôles pour tester les permissions RBAC :",
        "topbar_role_locked" to "Rôle verrouillé",
        "topbar_sync_pending" to "En attente de synchronisation",
        "topbar_synced" to "Synchronisé",
        "topbar_notifications" to "Notifications",

        // Dashboard & Metrics
        "metric_capital" to "Capital de Roulement",
        "metric_active_loans" to "Crédits en Cours",
        "metric_disbursed" to "Total Décaissé",
        "metric_collected" to "Total Recouvré",
        "metric_remaining" to "Reste à Recouvrer",
        "metric_recovery_rate" to "Taux de Recouvrement",
        "metric_profit_net" to "Bénéfice Net",
        "metric_cash_balance" to "Solde en Caisse",
        "metric_late_loans" to "Prêts en Retard",
        "dash_portfolio_title" to "Portefeuille & Performances",
        "dash_financial_performance" to "Performance Financière Globale",
        "dash_quick_actions" to "Actions Rapides",
        "dash_daily_recovery" to "Recouvrement Journalier (3,5%)",
        "dash_new_loan" to "Nouveau Prêt",
        "dash_record_payment" to "Encaisser",
        "dash_cash_in" to "Entrée Caisse",
        "dash_cash_out" to "Sortie Caisse",
        "dash_alert_late" to "Alerte : Crédits en Retard",

        // Clients Screen & Dialog
        "clients_search_placeholder" to "Rechercher client, téléphone, zone, ID...",
        "clients_count" to "client(s)",
        "clients_late_count" to "en retard",
        "clients_all_up_to_date" to "Tous à jour",
        "clients_empty" to "Aucun client trouvé pour cette recherche.",
        "clients_dialog_add_title" to "Nouveau Client",
        "clients_field_fullname" to "Nom complet *",
        "clients_field_phone" to "Numéro de téléphone *",
        "clients_field_gender" to "Sexe / Genre *",
        "gender_male" to "Homme (Masculin)",
        "gender_female" to "Femme (Féminin)",
        "clients_field_market" to "Marché / Zone *",
        "clients_field_activity" to "Type d'activité commerciale *",
        "clients_field_idcard" to "N° Pièce d'identité / Électeur",
        "clients_field_guarantor" to "Nom du Garant / Référence",
        "clients_field_guarantor_phone" to "Téléphone du Garant",
        "clients_field_notes" to "Observations / Emplacement stand",

        // Client Detail Screen
        "client_detail_title" to "Dossier Client",
        "client_detail_info_section" to "INFORMATIONS D'EXPLOITATION",
        "client_detail_activity" to "Activité",
        "client_detail_zone" to "Zone / Marché",
        "client_detail_phone" to "Téléphone",
        "client_detail_idcard" to "Pièce d'identité",
        "client_detail_guarantor_section" to "GARANT / RÉFÉRENCE",
        "client_detail_notes_section" to "NOTES & OBSERVATIONS",
        "client_detail_not_specified" to "Non renseigné",
        "client_detail_pay_late" to "Encaisser l'arriéré",
        "client_detail_pay_today" to "Encaisser l'échéance du jour",

        // Caisse
        "caisse_balance" to "Solde Actuel en Caisse",
        "caisse_total_in" to "Total Entrées",
        "caisse_total_out" to "Total Sorties",
        "caisse_history" to "Historique des Mouvements",
        "caisse_add_in" to "Enregistrer une Entrée",
        "caisse_add_out" to "Enregistrer une Sortie",
        "caisse_amount" to "Montant (FC)",
        "caisse_reason" to "Motif / Description",
        "caisse_category" to "Catégorie",
        "caisse_empty" to "Aucun mouvement enregistré.",

        // Cycles
        "cycle_title" to "Cycles de Crédit",
        "cycle_principal" to "Principal",
        "cycle_total_due" to "Total Dû",
        "cycle_collected" to "Recouvré",
        "cycle_remaining" to "Solde Restant",
        "cycle_daily_amount" to "Échéance / jour",
        "cycle_schedule" to "Échéancier (30 jours)",
        "cycle_late_alert" to "Crédits en Retard",
        "cycle_search_placeholder" to "Rechercher par client, cycle ID (ex: CY0001)...",

        // Reports
        "report_title" to "Rapports & Clôture Journalière",
        "report_recovery_title" to "Rapports de Recouvrement",
        "report_daily_summary" to "Bilan de la Journée",
        "report_share" to "Partager le Rapport",
        "report_gross_profit" to "Bénéfice Brut",
        "report_net_profit" to "Bénéfice Net Institution",
        "report_client_savings" to "Épargne Rétrocédée aux Clients",

        // More Menu
        "more_title" to "Administration & Modules Avancés",
        "more_subtitle" to "Outils de gestion et supervision de l'institution",
        "more_item_settings_title" to "Paramètres & Thème d'Affichage",
        "more_item_settings_desc" to "Mode clair/sombre, langue et barèmes financiers",
        "more_item_apps_title" to "Demandes de Prêt",
        "more_item_apps_desc" to "Étudier, approuver et décaisser les dossiers clients",
        "more_item_users_title" to "Gestion des Utilisateurs & Rôles",
        "more_item_users_desc" to "Ajouter des agents, assigner des zones et configurer les accès",
        "more_item_proj_title" to "Projection sur 6 Cycles",
        "more_item_proj_desc" to "Simulation de croissance du capital et des bénéfices",
        "more_item_audit_title" to "Journal d'Audit & Sécurité",
        "more_item_audit_desc" to "Traçabilité des opérations, suppressions et alertes",
        "more_item_doc_title" to "Aide & Documentation Métier",
        "more_item_doc_desc" to "Règles opérationnelles du microcrédit 3,5%/jour",
        "more_item_logout_title" to "Se déconnecter",
        "more_item_logout_desc" to "Déconnexion de l'application",

        // Client Portal
        "portal_welcome" to "Bienvenue sur votre espace",
        "portal_my_loan" to "Votre Prêt Actuel",
        "portal_my_savings" to "Votre Épargne Sécurisée",
        "portal_apply_btn" to "Demander un Nouveau Prêt",
        "portal_schedule_btn" to "Voir mon Échéancier",
        "portal_advisor_btn" to "Discuter avec mon Conseiller",
        "portal_no_active_loan" to "Vous n'avez pas de crédit en cours actuellement.",

        // Loan Apply
        "apply_title" to "Nouvelle Demande de Microcrédit",
        "apply_amount" to "Montant Souhaité (100.000 à 400.000 FC)",
        "apply_market" to "Votre Marché d'activité",
        "apply_activity" to "Votre Commerce / Activité",
        "apply_submit" to "Soumettre la Demande",

        // Common Actions
        "action_back" to "Retour",
        "action_cancel" to "Annuler",
        "action_confirm" to "Confirmer",
        "action_save" to "Enregistrer",
        "action_filter" to "Filtrer",
        "action_search" to "Rechercher...",
        "action_sync" to "Synchroniser",
        "action_sync_done" to "Données synchronisées",
        "action_sync_pending" to "En attente de synchronisation",
        "action_view" to "Voir",
        "action_contact" to "Contacter",
        "action_dossier" to "Dossier",
        "action_close" to "Fermer"
    )

    private val swahiliStrings = mapOf(
        // App Identity
        "app_title" to "MALISHO NA AKIBA",
        "app_acronym" to "\"MALAKI\"",
        "app_tagline" to "Mpango wa Mikopo na Akiba ya Wafanyabiashara\nMasoko ya Goma • Malipo ya Kila Siku 3,5%",

        // Navigation
        "nav_dashboard" to "Mwanzo",
        "nav_clients" to "Wateja",
        "nav_cycles" to "Mikopo",
        "nav_caisse" to "Kechi",
        "nav_reports" to "Ripoti",
        "nav_settings" to "Mipangilio",
        "nav_collect" to "Kukusanya",
        "nav_chat" to "Chat",
        "nav_apply" to "Omba Mkopo",
        "nav_schedule" to "Ratiba",
        "nav_advisor" to "Chat Mshauri",
        "nav_home" to "Nyumbani",

        // Screen Titles
        "title_dashboard" to "Dashbodi Kuu ya MALAKI",
        "title_clients" to "Orodha ya Wafanyabiashara",
        "title_client_detail" to "Habari za Mteja",
        "title_cycles" to "Mizunguko ya Mikopo",
        "title_cycle_detail" to "Habari za Mkopo",
        "title_applications" to "Maombi Mapya ya Mikopo",
        "title_record_payment" to "Kupokea Malipo Sokoni",
        "title_caisse" to "Sanduku Kuu la Makuta (Kechi)",
        "title_reports" to "Ripoti za Kazi na Faida",
        "title_client_portal" to "Eneo la Mteja",
        "title_client_schedule" to "Ratiba Yangu ya Kulipa",
        "title_client_loan_apply" to "Kuomba Mkopo Mpya",
        "title_chat" to "Chat",
        "title_notifications" to "Kituo cha Matangazo na Vikumbusho",
        "title_user_management" to "Usimamizi wa Wafanyakazi",
        "title_projections" to "Utabiri wa Mizunguko 6",
        "title_settings" to "Mipangilio na Chaguzi",
        "title_audit" to "Uchunguzi na Usalama wa Mfumo",
        "title_more" to "Usimamizi na Vyombo",
        "title_agent_home" to "Ukurasa wa Afisa wa Soko",

        // Settings
        "settings_language_section" to "UCHAGUZI WA LUGHA YA MFUMO",
        "settings_language_desc" to "Chagua lugha unayopendelea kutumia mu programu : Kifaransa ao Kiswahili chepesi cha masoko ya DRC :",
        "settings_lang_fr" to "Kifaransa (Français)",
        "settings_lang_sw" to "Kiswahili (RDC)",
        "settings_lang_active" to "Lugha inayotumika",
        "settings_theme_section" to "MUUONEKANO WA KIOO (MWANGA NA GIZA)",
        "settings_theme_desc" to "Chagua namna kioo kinaonekana ili uone vizuri ukiwa sokoni ao ku ofisi :",
        "settings_theme_light" to "Mwanga (Clair)",
        "settings_theme_dark" to "Giza (Sombre)",
        "settings_theme_auto" to "Kufuatana na Simu",
        "settings_google_section" to "AKAUNTI YA GOOGLE INAYOFANYA KAZI",
        "settings_google_logout" to "Kutoka mu akaunti ya Google",
        "settings_business_section" to "JINA NA MAELEZO YA SHIRIKA",
        "settings_org_name" to "Jina la Shirika la Mikopo",
        "settings_initial_cap" to "Mtaji wa Mwanzo (FC)",
        "settings_target_cap" to "Mtaji Unaotafutwa (FC)",
        "settings_rates_section" to "FAIDA NA SIKU ZA MIKOPO",
        "settings_daily_rate" to "Faida ya kila siku (%)",
        "settings_duration_days" to "Siku za mkopo (mzunguko)",
        "settings_profit_split" to "MGAWANYO WA FAIDA NA AKIBA",
        "settings_share_company" to "Sehemu ya Shirika (%)",
        "settings_share_client" to "Sehemu ya Mteja / Akiba (%)",
        "settings_expense_rate" to "Gharama za kazi (%)",
        "settings_loss_rate" to "Akiba ya hasara (%)",
        "settings_save" to "Chunga Mipangilio Hii",
        "settings_modules_section" to "SEHEMU ZINGINE ZA USIMAMIZI",
        "settings_saved_success" to "Mipangilio imechungwa vizuri !",

        // Roles & Auth
        "role_manager" to "Usimamizi Mkuu (Mkurugenzi)",
        "role_agent" to "Afisa wa Soko (Agent)",
        "role_cashier" to "Mweka Hazina (Caissier)",
        "role_client" to "Mteja",
        "auth_choose_account" to "Chagua akaunti",
        "auth_continue_to" to "ili kuendelea na MALISHO NA AKIBA (MALAKI)",
        "auth_signin_google" to "Ingia na Google",
        "auth_or_credentials" to "AO INGIA NA JINA NA PIN",
        "auth_login" to "Ingia mu Programu",
        "auth_username" to "Jina la kuingilia",
        "auth_pin" to "Nambari ya siri PIN (namba 4)",
        "auth_logout" to "Kutoka mu Programu",
        "auth_biometric" to "Kuingia na Alama ya Kidole",
        "auth_biometric_locked" to "Programu Imefungwa kwa Alama ya Kidole",
        "auth_biometric_success" to "Alama ya kidole imetambuliwa vizuri",
        "auth_biometric_fail" to "Alama ya kidole haikutambuliwa",
        "auth_biometric_pin_fallback" to "Tumia PIN ya siri",

        // Top Bar & RBAC Dialog
        "topbar_role_title" to "Eneo la Mkurugenzi : Madaraka na Kuingia",
        "topbar_google_account" to "Akaunti ya Google inayotumika :",
        "topbar_switch_role_desc" to "Badilisha madaraka ili ujaribu ruhusa za kila mfanyakazi :",
        "topbar_role_locked" to "Jukumu limefungwa",
        "topbar_sync_pending" to "Inangoja kutumwa",
        "topbar_synced" to "Imetumwa vizuri",
        "topbar_notifications" to "Matangazo",

        // Dashboard & Metrics
        "metric_capital" to "Mtaji Unaofanya Kazi",
        "metric_active_loans" to "Mikopo Inayoendelea",
        "metric_disbursed" to "Jumla Iliyotolewa",
        "metric_collected" to "Jumla Iliyolipwa",
        "metric_remaining" to "Deni Inayobaki",
        "metric_recovery_rate" to "Kipimo cha Malipo",
        "metric_profit_net" to "Faida Safi ya Shirika",
        "metric_cash_balance" to "Makuta Yaliyo mu Kechi",
        "metric_late_loans" to "Mikopo Iliyochelewa",
        "dash_portfolio_title" to "Hali ya Mikopo na Matokeo",
        "dash_financial_performance" to "Uwezo wa Makuta na Faida",
        "dash_quick_actions" to "Kazi za Haraka",
        "dash_daily_recovery" to "Kukusanya kwa Kila Siku (3,5%)",
        "dash_new_loan" to "Mkopo Mpya",
        "dash_record_payment" to "Kukusanya Malipo",
        "dash_cash_in" to "Kuingiza mu Sanduku",
        "dash_cash_out" to "Kutoa mu Sanduku",
        "dash_alert_late" to "Onyo : Mikopo Iliyochelewa",

        // Clients Screen & Dialog
        "clients_search_placeholder" to "Tafuta mteja, simu, soko, ID...",
        "clients_count" to "wateja",
        "clients_late_count" to "wamechelewa",
        "clients_all_up_to_date" to "Wote wako sawa",
        "clients_empty" to "Hakuna mteja anayelingana na utafutaji huu.",
        "clients_dialog_add_title" to "Mteja Mpya Sokoni",
        "clients_field_fullname" to "Jina kamili *",
        "clients_field_phone" to "Namba ya simu *",
        "clients_field_gender" to "Jinsia *",
        "gender_male" to "Mwanaume",
        "gender_female" to "Mwanamke",
        "clients_field_market" to "Soko / Eneo *",
        "clients_field_activity" to "Aina ya biashara *",
        "clients_field_idcard" to "Nambari ya Kitambulisho",
        "clients_field_guarantor" to "Jina la Mdhamini (Garant)",
        "clients_field_guarantor_phone" to "Simu ya Mdhamini",
        "clients_field_notes" to "Maelezo / Eneo la Meza",

        // Client Detail Screen
        "client_detail_title" to "Habari za Mteja",
        "client_detail_info_section" to "HABARI ZA KAZI SOKONI",
        "client_detail_activity" to "Aina ya Kazi",
        "client_detail_zone" to "Soko / Eneo",
        "client_detail_phone" to "Nambari ya Simu",
        "client_detail_idcard" to "Kitambulisho",
        "client_detail_guarantor_section" to "MDHAMINI WA MKOPO (GARANT)",
        "client_detail_notes_section" to "MAELEZO NA UANGALIZI",
        "client_detail_not_specified" to "Haijaandikwa",
        "client_detail_pay_late" to "Kukusanya arriéré",
        "client_detail_pay_today" to "Kupokea malipo ya leo",

        // Caisse
        "caisse_balance" to "Makuta Yaliyo mu Sanduku Sasa",
        "caisse_total_in" to "Jumla Iliyoingia",
        "caisse_total_out" to "Jumla Iliyotoka",
        "caisse_history" to "Historia ya Makuta mu Sanduku",
        "caisse_add_in" to "Kuingiza Makuta mu Sanduku",
        "caisse_add_out" to "Kutoa Makuta mu Sanduku",
        "caisse_amount" to "Kiasi ya Makuta (FC)",
        "caisse_reason" to "Sababu ya Muamala",
        "caisse_category" to "Aina ya Muamala",
        "caisse_empty" to "Hakuna muamala wowote uliorekodiwa.",

        // Cycles
        "cycle_title" to "Mizunguko ya Mikopo",
        "cycle_principal" to "Mtaji Uliotolewa",
        "cycle_total_due" to "Jumla ya Kulipa",
        "cycle_collected" to "Iliyokusanywa",
        "cycle_remaining" to "Inayobaki",
        "cycle_daily_amount" to "Malipo ya Siku",
        "cycle_schedule" to "Ratiba ya Siku 30",
        "cycle_late_alert" to "Mikopo Iliyochelewa",
        "cycle_search_placeholder" to "Tafuta kwa mteja, namba ya mkopo (mfano: CY0001)...",

        // Reports
        "report_title" to "Ripoti na Kufunga Hesabu ya Siku",
        "report_recovery_title" to "Ripoti za Malipo ya Mikopo",
        "report_daily_summary" to "Hesabu ya Kazi ya Leo",
        "report_share" to "Kutuma Ripoti Hii",
        "report_gross_profit" to "Faida ya Jumla",
        "report_net_profit" to "Faida Safi ya Shirika",
        "report_client_savings" to "Akiba Iliyowekwa kwa Wateja",

        // More Menu
        "more_title" to "Usimamizi Mkuu na Vyombo vya Kazi",
        "more_subtitle" to "Vyombo vya kuongoza na kusimamia kazi za MALAKI",
        "more_item_settings_title" to "Mipangilio na Mwonekano wa Kioo",
        "more_item_settings_desc" to "Mwanga/giza, lugha na viwango vya mikopo",
        "more_item_apps_title" to "Maombi ya Mikopo",
        "more_item_apps_desc" to "Kuangalia, kukubali na kutoa mikopo kwa wateja",
        "more_item_users_title" to "Usimamizi wa Wafanyakazi na Majukumu",
        "more_item_users_desc" to "Kuongeza ma-agent, kugawa masoko na madaraka",
        "more_item_proj_title" to "Utabiri wa Mizunguko 6 ya Mikopo",
        "more_item_proj_desc" to "Kipimo cha kukua kwa mtaji na faida ya shirika",
        "more_item_audit_title" to "Kitabu cha Ukaguzi na Usalama",
        "more_item_audit_desc" to "Kufuatilia kila kazi iliyofanyika na usalama",
        "more_item_doc_title" to "Msaada na Maelezo ya Kazi",
        "more_item_doc_desc" to "Kanuni za mikopo ya faida 3,5% kwa siku",
        "more_item_logout_title" to "Kutoka mu Akaunti",
        "more_item_logout_desc" to "Kufunga na kutoka mu mfumo",

        // Client Portal
        "portal_welcome" to "Karibu mu eneo lako",
        "portal_my_loan" to "Mkopo Wako Unaofanya Kazi",
        "portal_my_savings" to "Akiba Yako Iliyochungwa",
        "portal_apply_btn" to "Kuomba Mkopo Mpya",
        "portal_schedule_btn" to "Angalia Ratiba ya Malipo",
        "portal_advisor_btn" to "Zungumuza na Mshauri Wako",
        "portal_no_active_loan" to "Kwa sasa hauna mkopo wowote unaoendelea.",

        // Loan Apply
        "apply_title" to "Kuomba Mkopo Mpya wa Biashara",
        "apply_amount" to "Kiasi Unachoomba (100.000 mpaka 400.000 FC)",
        "apply_market" to "Soko Lako la Biashara",
        "apply_activity" to "Kazi / Biashara Yako",
        "apply_submit" to "Tuma Ombi Lako",

        // Common Actions
        "action_back" to "Kurudi",
        "action_cancel" to "Acha",
        "action_confirm" to "Kukubali",
        "action_save" to "Kuchunga",
        "action_filter" to "Kuchuja",
        "action_search" to "Kutafuta...",
        "action_sync" to "Kutuma data",
        "action_sync_done" to "Data imetumwa vizuri",
        "action_sync_pending" to "Inangoja kutumwa",
        "action_view" to "Angalia",
        "action_contact" to "Wasiliana",
        "action_dossier" to "Faili",
        "action_close" to "Kufunga"
    )
}
