import SwiftUI
import shared

/// Mirrors androidApp's TripListScreen: Upcoming/Past sections plus the
/// persistent "Next up" banner. Both platforms read the same
/// TripListViewModel from the shared module, so this file's only job is
/// laying the state out with native SwiftUI widgets.
struct TripListView: View {
    @StateObject private var observer = TripListObserver()

    var body: some View {
        NavigationStack {
            Group {
                if observer.state.isLoading {
                    ProgressView()
                } else if observer.state.upcomingTrips.isEmpty && observer.state.pastTrips.isEmpty {
                    EmptyTripsView()
                } else {
                    List {
                        OfflineBannerView(syncStatus: observer.state.syncStatus)
                            .listRowInsets(EdgeInsets())
                            .listRowSeparator(.hidden)
                        if let banner = observer.state.nextActivityBanner {
                            NextUpBannerView(banner: banner)
                                .listRowInsets(EdgeInsets())
                                .listRowSeparator(.hidden)
                        }
                        if !observer.state.upcomingTrips.isEmpty {
                            Section("Upcoming") {
                                ForEach(observer.state.upcomingTrips, id: \.id) { trip in
                                    NavigationLink(value: trip.id) {
                                        TripRow(trip: trip)
                                    }
                                }
                            }
                        }
                        if !observer.state.pastTrips.isEmpty {
                            Section("Past") {
                                ForEach(observer.state.pastTrips, id: \.id) { trip in
                                    NavigationLink(value: trip.id) {
                                        TripRow(trip: trip)
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("TripMate")
            .navigationDestination(for: String.self) { tripId in
                TripDetailView(tripId: tripId)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { /* TODO: present create-trip sheet */ }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
    }
}

private struct TripRow: View {
    let trip: Trip

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(trip.title).font(.headline)
            Text(trip.destination).font(.subheadline).foregroundStyle(.secondary)
            Text("\(trip.startDate.toKotlinDisplayString()) – \(trip.endDate.toKotlinDisplayString())")
                .font(.footnote)
                .foregroundStyle(Color("DeepTeal"))
        }
        .padding(.vertical, 4)
    }
}

private struct EmptyTripsView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("No trips yet").font(.title3.bold())
            Text("Plan your first trip — TripMate will remind you what's next, wherever you are.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
        }
        .padding(32)
    }
}
