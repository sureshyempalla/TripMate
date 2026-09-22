import SwiftUI
import shared

/// Mirrors androidApp's AddActivityScreen: a single form rather than a
/// multi-step wizard, since every field here is quick to fill in on the go.
struct AddActivityView: View {
    let tripId: String
    @Environment(\.dismiss) private var dismiss
    @StateObject private var observer: AddActivityObserver

    init(tripId: String) {
        self.tripId = tripId
        _observer = StateObject(wrappedValue: AddActivityObserver(tripId: tripId))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("What's the activity?", text: Binding(
                        get: { observer.state.title },
                        set: { observer.viewModel.onTitleChanged(title: $0) }
                    ))
                    Picker("Category", selection: Binding(
                        get: { observer.state.category },
                        set: { observer.viewModel.onCategoryChanged(category: $0) }
                    )) {
                        ForEach(ActivityCategory.entries, id: \.self) { category in
                            Text(category.name.capitalized).tag(category)
                        }
                    }
                    TextField("Place", text: Binding(
                        get: { observer.state.place?.name ?? "" },
                        set: { text in
                            observer.viewModel.onPlaceChanged(
                                place: text.isEmpty ? nil : Place(
                                    name: text, address: nil, latitude: nil,
                                    longitude: nil, providerPlaceId: nil
                                )
                            )
                        }
                    ))
                }

                Section("Reminder") {
                    Picker("Remind me", selection: Binding(
                        get: { Int32(observer.state.reminderLeadMinutes) },
                        set: { observer.viewModel.onReminderLeadChanged(minutes: $0) }
                    )) {
                        Text("15m before").tag(Int32(15))
                        Text("30m before").tag(Int32(30))
                        Text("1h before").tag(Int32(60))
                        Text("2h before").tag(Int32(120))
                    }
                }

                Section("Notes") {
                    TextField("Notes", text: Binding(
                        get: { observer.state.notes },
                        set: { observer.viewModel.onNotesChanged(notes: $0) }
                    ), axis: .vertical)
                    .lineLimit(3...6)
                }

                if let error = observer.state.error {
                    Text(error).foregroundStyle(.red)
                }
            }
            .navigationTitle("Add activity")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { observer.viewModel.save() }
                        .disabled(observer.state.isSaving)
                }
            }
            .onChange(of: observer.state.savedSuccessfully) { _, saved in
                if saved { dismiss() }
            }
        }
    }
}
