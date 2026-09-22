import Foundation
import shared

/// KMM ViewModels expose Kotlin `StateFlow`s, which SwiftUI can't observe
/// directly. Each Observer subscribes to one ViewModel's flow via
/// SKIE/native `watch` (see the SKIE note in the README) and republishes it
/// as `@Published`, so SwiftUI views just do `@StateObject` + normal
/// property access like any other ObservableObject.
///
/// These assume the Kotlin CocoaPods/SKIE export turns `Flow<T>` into
/// something Swift can `for await` over (`asAsyncSequence` via SKIE, or a
/// hand-rolled `NativeFlowWrapper` if SKIE isn't used) — swap the
/// `watch()` calls below for whichever bridging approach the project uses.

@MainActor
final class TripListObserver: ObservableObject {
    // NOTE: `.offline` assumes SKIE's enum-case lowercasing (see the SKIE
    // note above) — without SKIE, Kotlin/Native's default Obj-C export
    // keeps enum cases upper-cased (`.OFFLINE`); check Xcode's generated
    // interface for `SyncStatus` on first build.
    @Published var state = TripListUiState(
        isLoading: true, upcomingTrips: [], pastTrips: [], nextActivityBanner: nil, syncStatus: .syncing
    )

    private let viewModel = KoinHelper.shared.tripListViewModel(userId: "current_user")
    private var task: Task<Void, Never>?

    init() {
        task = Task {
            for await value in viewModel.uiState {
                state = value
            }
        }
    }

    deinit { task?.cancel() }
}

@MainActor
final class TripDetailObserver: ObservableObject {
    @Published var state = TripDetailUiState(isLoading: true, trip: nil, days: [], syncStatus: .syncing)
    private let viewModel: TripDetailViewModel
    private var task: Task<Void, Never>?

    init(tripId: String) {
        viewModel = KoinHelper.shared.tripDetailViewModel(tripId: tripId)
        task = Task {
            for await value in viewModel.uiState {
                state = value
            }
        }
    }

    deinit { task?.cancel() }
}

@MainActor
final class AddActivityObserver: ObservableObject {
    @Published var state = AddActivityFormState(
        title: "", category: .activity, place: nil, startAtEpochMillis: nil,
        endAtEpochMillis: nil, notes: "", reminderLeadMinutes: 30,
        isSaving: false, error: nil, savedSuccessfully: false
    )
    let viewModel: AddActivityViewModel
    private var task: Task<Void, Never>?

    init(tripId: String) {
        viewModel = KoinHelper.shared.addActivityViewModel(tripId: tripId)
        task = Task {
            for await value in viewModel.formState {
                state = value
            }
        }
    }

    deinit { task?.cancel() }
}
