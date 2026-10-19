package com.example.data.model

enum class UserRole(val label: String) {
    GESTIONNAIRE("Gestionnaire / Propriétaire"),
    AGENT("Agent de terrain"),
    CAISSIER("Caissier"),
    CLIENT("Client")
}

enum class LoanStatus(val label: String) {
    BROUILLON("Brouillon"),
    SOUMISE("Soumise"),
    EN_ETUDE("En cours d'étude"),
    APPROUVEE("Approuvée"),
    REFUSEE("Refusée"),
    DECAISSEE("Décaissement effectué"),
    EN_COURS("En cours de remboursement"),
    TERMINEE("Terminée")
}

enum class CycleStatus(val label: String) {
    PREVU("Prévu"),
    APPROUVE("Approuvé"),
    DECAISSE("Décaissé"),
    EN_COURS("En cours"),
    EN_RETARD("En retard"),
    TERMINE("Terminé"),
    ANNULE("Annulé"),
    EN_DEFAUT("En défaut")
}

enum class PaymentMethod(val label: String) {
    ESPECES("Espèces"),
    MOBILE_MONEY("Mobile Money"),
    BANQUE("Banque"),
    AUTRE("Autre")
}

enum class CashMovementType(val label: String) {
    ENTREE("Entrée"),
    SORTIE("Sortie")
}

enum class CashMovementCategory(val label: String) {
    REMBOURSEMENT("Remboursement client"),
    APPORT_CAPITAL("Apport de capital"),
    DEPOT("Dépôt bancaire/caisse"),
    DECAISSEMENT_PRET("Décaissement d'un prêt"),
    CHARGE("Charge d'exploitation"),
    SALAIRE("Salaire du personnel"),
    TRANSPORT("Frais de transport"),
    ACHAT("Achat fournitures/équipements"),
    AUTRE("Autre opération")
}

enum class NotificationType(val label: String) {
    DEMANDE("Demande de prêt"),
    PAIEMENT("Paiement enregistré"),
    RETARD("Alerte retard"),
    DECAISSEMENT("Décaissement prêt"),
    CAISSE("Mouvement caisse"),
    MESSAGE("Nouveau message"),
    SYSTEM("Système")
}
