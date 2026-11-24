package io.github.hdghg.sevenhandshakes.di

import io.github.hdghg.sevenhandshakes.ui.addeditidentity.AddEditIdentityViewModel
import io.github.hdghg.sevenhandshakes.ui.contactdetail.ContactDetailViewModel
import io.github.hdghg.sevenhandshakes.ui.contactlink.ContactLinkViewModel
import io.github.hdghg.sevenhandshakes.ui.contacts.ContactsViewModel
import io.github.hdghg.sevenhandshakes.ui.export.ExportViewModel
import io.github.hdghg.sevenhandshakes.ui.feed.FeedViewModel
import io.github.hdghg.sevenhandshakes.ui.graphinspection.GraphInspectionViewModel
import io.github.hdghg.sevenhandshakes.ui.identities.IdentitiesViewModel
import io.github.hdghg.sevenhandshakes.ui.importdata.ImportViewModel
import io.github.hdghg.sevenhandshakes.ui.importselection.ImportSelectionViewModel
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactViewModel
import io.github.hdghg.sevenhandshakes.ui.newgratitude.NewGratitudeViewModel
import io.github.hdghg.sevenhandshakes.ui.reviewdetail.ReviewDetailViewModel
import io.github.hdghg.sevenhandshakes.ui.reviewlink.ReviewLinkViewModel
import io.github.hdghg.sevenhandshakes.ui.reviewrequest.ReviewRequestViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Every screen's ViewModel. A SavedStateHandle parameter gets the screen's navigation arguments. */
val viewModelModule = module {
    viewModelOf(::AddEditIdentityViewModel)
    viewModelOf(::ContactDetailViewModel)
    viewModelOf(::ContactLinkViewModel)
    viewModelOf(::ContactsViewModel)
    viewModelOf(::GraphInspectionViewModel)
    viewModelOf(::IdentitiesViewModel)
    viewModelOf(::NewContactViewModel)
    viewModelOf(::NewGratitudeViewModel)
    viewModelOf(::ReviewDetailViewModel)
    viewModelOf(::ReviewRequestViewModel)

    // These take a named dispatcher, so their parameters are listed.
    viewModel {
        ExportViewModel(
            contactRepository = get(),
            reviewRepository = get(),
            context = androidContext(),
            ioDispatcher = get(IoDispatcher),
        )
    }
    viewModel {
        FeedViewModel(
            reviewRepository = get(),
            contactRepository = get(),
            identityRepository = get(),
            selectedIdentityRepository = get(),
            dispatcher = get(DefaultDispatcher),
        )
    }
    viewModel { ImportViewModel(context = androidContext(), ioDispatcher = get(IoDispatcher)) }
    viewModel {
        ImportSelectionViewModel(
            importer = get(),
            context = androidContext(),
            ioDispatcher = get(IoDispatcher),
            savedStateHandle = get(),
        )
    }
    viewModel {
        ReviewLinkViewModel(
            importer = get(),
            contactRepository = get(),
            dispatcher = get(DefaultDispatcher),
            savedStateHandle = get(),
        )
    }
}

/** All modules of the app, in the order Koin loads them. */
val appModules = listOf(
    coroutinesModule,
    databaseModule,
    dataStoreModule,
    repositoryModule,
    shareModule,
    viewModelModule,
)
