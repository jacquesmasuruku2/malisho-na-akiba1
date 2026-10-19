package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.CashMovementCategory
import com.example.data.model.CashMovementType
import com.example.data.model.CycleStatus
import com.example.data.model.LoanStatus
import com.example.data.model.NotificationType
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let {
        runCatching { UserRole.valueOf(it) }.getOrDefault(UserRole.CLIENT)
    }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus?): String? = value?.name

    @TypeConverter
    fun toLoanStatus(value: String?): LoanStatus? = value?.let {
        runCatching { LoanStatus.valueOf(it) }.getOrDefault(LoanStatus.SOUMISE)
    }

    @TypeConverter
    fun fromCycleStatus(value: CycleStatus?): String? = value?.name

    @TypeConverter
    fun toCycleStatus(value: String?): CycleStatus? = value?.let {
        runCatching { CycleStatus.valueOf(it) }.getOrDefault(CycleStatus.EN_COURS)
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let {
        runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.ESPECES)
    }

    @TypeConverter
    fun fromCashMovementType(value: CashMovementType?): String? = value?.name

    @TypeConverter
    fun toCashMovementType(value: String?): CashMovementType? = value?.let {
        runCatching { CashMovementType.valueOf(it) }.getOrDefault(CashMovementType.ENTREE)
    }

    @TypeConverter
    fun fromCashMovementCategory(value: CashMovementCategory?): String? = value?.name

    @TypeConverter
    fun toCashMovementCategory(value: String?): CashMovementCategory? = value?.let {
        runCatching { CashMovementCategory.valueOf(it) }.getOrDefault(CashMovementCategory.AUTRE)
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType?): String? = value?.name

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? = value?.let {
        runCatching { NotificationType.valueOf(it) }.getOrDefault(NotificationType.SYSTEM)
    }
}
