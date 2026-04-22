//
// CreativityStruct.swift
//

import Foundation
import SwiftUI
import sharedCode

struct CreativityStruct: View {
	@ObservedObject var viewModel: InputViewModel

	@State private var started: Bool = false
	@State private var ended: Bool = false
	@State private var remaining: Int = 0
	@State private var textValue: String = ""
	@State private var word: String = ""

	private var duration: Int { Int(viewModel.input.creativityDuration) }

	private var wordPool: [String] {
		let words = viewModel.input.creativityWords
			.split(separator: ",")
			.map { $0.trimmingCharacters(in: .whitespaces) }
			.filter { !$0.isEmpty }
		return words.isEmpty ? [
			"Toothpaste", "Foam Toothbrush", "Wine bottle", "Ping pong ball", "Headband",
			"Mirror", "Sock", "Rope", "Suitcase", "Umbrella", "Paintbrush", "Band-aid",
			"Trash can", "Spoon", "Ruler", "Lamp", "Tie", "Toilet paper", "Coat hook",
			"Car tire", "Bathtub", "Banana peel", "Flower vase", "Iron", "Paper clip",
			"Lighter", "Hairdryer", "Frisbee", "Fork", "Watering can", "Bell", "Belt",
			"Horseshoe", "Hat"
		] : words
	}

	var body: some View {
		Group {
			if !viewModel.value.isEmpty {
				completedView
			} else if !started {
				startView
			} else {
				activeView
			}
		}
		.onAppear {
			if word.isEmpty {
				word = pickWord()
			}
			remaining = duration
		}
	}

	private var completedView: some View {
		HStack {
			Spacer()
			Text("Creativity test complete.")
				.foregroundColor(.green)
			Spacer()
		}
		.padding()
	}

	private var startView: some View {
		VStack(spacing: 12) {
			Text(viewModel.input.creativityInstructions)
				.font(.footnote)
				.foregroundColor(.secondary)
				.multilineTextAlignment(.center)
			Text("You will have \(duration) seconds once you start.")
				.font(.footnote)
				.foregroundColor(.secondary)
			Button("Start") {
				started = true
			}
			.buttonStyle(.borderedProminent)
		}
		.frame(maxWidth: .infinity)
		.padding()
	}

	private var activeView: some View {
		VStack(alignment: .leading, spacing: 12) {
			ProgressView(value: Double(remaining), total: Double(duration))
				.tint(remaining <= 3 ? .red : .accentColor)

			HStack {
				Text(viewModel.input.creativityInstructions)
					.font(.footnote)
					.foregroundColor(.secondary)
				Spacer()
				Text("\(remaining)s")
					.font(.title3)
					.fontWeight(.bold)
					.foregroundColor(remaining <= 3 ? .red : .accentColor)
			}

			Text(word)
				.font(.system(size: 28, weight: .heavy))
				.frame(maxWidth: .infinity)
				.padding(.vertical, 20)
				.background(Color.accentColor.opacity(0.15))
				.cornerRadius(16)
				.multilineTextAlignment(.center)

			TextEditor(text: $textValue)
				.frame(minHeight: 140)
				.disabled(ended)
				.overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))
		}
		.padding()
		.task(id: started) {
			guard started else { return }
			while remaining > 0 && !ended {
				try? await Task.sleep(nanoseconds: 1_000_000_000)
				remaining -= 1
			}
			if !ended {
				ended = true
				viewModel.setAdditionalValue(value: textValue, additionalValues: ["word": word])
			}
		}
	}

	private func pickWord() -> String {
		let defaults = UserDefaults.standard
		let today = {
			let formatter = DateFormatter()
			formatter.dateFormat = "yyyy-MM-dd"
			return formatter.string(from: Date())
		}()
		let key = "esmira_creativity_used_words_\(today)"
		let usedRaw = defaults.string(forKey: key) ?? ""
		let used = usedRaw.isEmpty ? [] : usedRaw.split(separator: "|").map(String.init)
		let available = wordPool.filter { !used.contains($0) }
		let pool = available.isEmpty ? wordPool : available
		let picked = pool.randomElement() ?? pool[0]
		let updated = available.isEmpty ? [picked] : used + [picked]
		defaults.set(updated.joined(separator: "|"), forKey: key)
		return picked
	}
}
