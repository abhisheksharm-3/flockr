package `in`.xroden.flockr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.xroden.flockr.features.auth.data.AuthRepository
import `in`.xroden.flockr.features.auth.data.IAuthRepository
import `in`.xroden.flockr.features.chores.data.ChoreRepository
import `in`.xroden.flockr.features.chores.data.IChoreRepository
import `in`.xroden.flockr.features.chat.data.ChatRepository
import `in`.xroden.flockr.features.chat.data.IChatRepository
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import `in`.xroden.flockr.features.documents.data.IDocumentRepository
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.data.IExpenseRepository
import `in`.xroden.flockr.features.expenses.data.IPerDiemRepository
import `in`.xroden.flockr.features.expenses.data.IRecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.data.ITransactionRepository
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.expenses.data.RecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.data.TransactionRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
import `in`.xroden.flockr.features.notifications.data.INotificationRepository
import `in`.xroden.flockr.features.shopping.data.ShoppingRepository
import `in`.xroden.flockr.features.shopping.data.IShoppingRepository
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindHouseRepository(
        houseRepository: HouseRepository
    ): IHouseRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepository: ExpenseRepository
    ): IExpenseRepository

    @Binds
    @Singleton
    abstract fun bindPerDiemRepository(
        perDiemRepository: PerDiemRepository
    ): IPerDiemRepository

    @Binds
    @Singleton
    abstract fun bindRecurringExpenseRepository(
        recurringExpenseRepository: RecurringExpenseRepository
    ): IRecurringExpenseRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepository: TransactionRepository
    ): ITransactionRepository

    @Binds
    @Singleton
    abstract fun bindChoreRepository(
        choreRepository: ChoreRepository
    ): IChoreRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepository: ChatRepository
    ): IChatRepository

    @Binds
    @Singleton
    abstract fun bindShoppingRepository(
        shoppingRepository: ShoppingRepository
    ): IShoppingRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepository: DocumentRepository
    ): IDocumentRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepository: NotificationRepository
    ): INotificationRepository
}

