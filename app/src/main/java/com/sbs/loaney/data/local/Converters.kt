package com.sbs.loaney.data.local

import androidx.room.TypeConverter
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromLoanType(value: LoanType): String {
        return value.name
    }

    @TypeConverter
    fun toLoanType(value: String): LoanType {
        return LoanType.valueOf(value)
    }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String {
        return value.name
    }

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus {
        return LoanStatus.valueOf(value)
    }
}
