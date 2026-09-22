import SwiftUI
import shared

/// Timeline view: same day-grouped layout as androidApp's TripDetailScreen.
struct TripDetailView: View {
    let tripId: String
    @StateObject private var observer: TripDetailObserver
    @State private var showingAddActivity = false

    init(tripId: String) {
        self.tripId = tripId
        _observer = StateObject(wrappedValue: TripDetailObserver(tripId: tripId))
    }

    var body: some View {
        VStack(spacing: 0) {
            OfflineBannerView(syncStatus: observer.state.syncStatus)
            Group {
                if observer.state.isLoading {
                    ProgressView()
                } else if observer.state.days.isEmpty {
                    Text("No activities yet. Tap + to add the first one.")
                        .foregroundStyle(.secondary)
                } else {
                    List {
                        ForEach(observer.state.days, id: \.date) { day in
                            Section(day.date.toDisplayString()) {
                                ForEach(day.activities, id: \.id) { activity in
                                    ActivityRow(activity: activity)
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
        }
        .navigationTitle(observer.state.trip?.title ?? "Trip")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: { showingAddActivity = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAddActivity) {
            AddActivityView(tripId: tripId)
        }
    }
}

private struct ActivityRow: View {
    let activity: Activity

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(activity.title).font(.headline)
                if let place = activity.place {
                    Label(place.name, systemImage: "mappin.and.ellipse")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            Text(activity.startAtEpochMillis.toTimeLabel())
                .font(.subheadline.weight(.medium))
        }
        .padding(.vertical, 4)
    }
}
