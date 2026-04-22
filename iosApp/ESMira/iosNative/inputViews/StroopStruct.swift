//
// StroopStruct.swift
//

import Foundation
import SwiftUI
import sharedCode

private struct StroopTrialData {
	let word: String
	let inkColor: String
}

private struct TrialResult: Codable {
	let trial: Int
	let word: String
	let inkColor: String
	let response: String
	let responseTime: Int64
	let correct: Bool
}

struct StroopStruct: View {
	@ObservedObject var viewModel: InputViewModel

	@State private var colors: [String] = []
	@State private var trials: [StroopTrialData] = []
	@State private var started: Bool = false
	@State private var currentTrialIndex: Int = 0
	@State private var trialStartTime: Date = Date()
	@State private var results: [TrialResult] = []
	@State private var showingFixation: Bool = false

	var body: some View {
		Group {
			if !viewModel.value.isEmpty {
				completedView
			} else if trials.isEmpty {
				EmptyView()
			} else if !started {
				startView
			} else if currentTrialIndex >= trials.count {
				EmptyView()
			} else if showingFixation {
				fixationView
			} else {
				trialView(trial: trials[currentTrialIndex])
			}
		}
		.onAppear {
			guard trials.isEmpty else { return }
			setupTrials()
		}
	}

	private var startView: some View {
		VStack(spacing: 12) {
			Text("Stroop Test")
				.font(.headline)
			Button("Start") {
				trialStartTime = Date()
				started = true
			}
			.buttonStyle(.borderedProminent)
		}
		.frame(maxWidth: .infinity)
		.padding()
	}

	private var completedView: some View {
		HStack {
			Spacer()
			Text("Stroop test complete.")
				.foregroundColor(.green)
			Spacer()
		}
		.padding()
	}

	private var fixationView: some View {
		Text("+")
			.font(.system(size: 32))
			.frame(maxWidth: .infinity, minHeight: 160)
			.task {
				try? await Task.sleep(nanoseconds: 500_000_000)
				trialStartTime = Date()
				showingFixation = false
			}
	}

	private func trialView(trial: StroopTrialData) -> some View {
		VStack(spacing: 0) {
			Text("\(currentTrialIndex + 1) / \(trials.count)")
				.font(.caption)
				.foregroundColor(.secondary)
				.padding(.bottom, 24)

			Group {
				if viewModel.input.stroopShowWord {
					Text(trial.word.uppercased())
						.font(.system(size: 40, weight: .bold))
						.foregroundColor(stroopColor(trial.inkColor))
				} else {
					RoundedRectangle(cornerRadius: 8)
						.fill(stroopColor(trial.inkColor))
						.frame(width: 120, height: 50)
				}
			}
			.frame(height: 80)
			.padding(.bottom, 32)

			HStack(spacing: 8) {
				ForEach(colors, id: \.self) { color in
					Button(action: {
						let responseTime = Int64(Date().timeIntervalSince(trialStartTime) * 1000)
						let result = TrialResult(
							trial: currentTrialIndex + 1,
							word: trial.word,
							inkColor: trial.inkColor,
							response: color,
							responseTime: responseTime,
							correct: color == trial.inkColor
						)
						results.append(result)

						if currentTrialIndex + 1 >= trials.count {
							let correctCount = results.filter { $0.correct }.count
							let accuracy = correctCount * 100 / results.count
							let meanRT = results.map { $0.responseTime }.reduce(0, +) / Int64(results.count)
							if let data = try? JSONEncoder().encode(results),
							   let jsonString = String(data: data, encoding: .utf8) {
								viewModel.setAdditionalValue(
									value: jsonString,
									additionalValues: [
										"accuracy": "\(accuracy)",
										"meanRT": "\(meanRT)"
									]
								)
							}
						} else {
							currentTrialIndex += 1
							showingFixation = true
						}
					}) {
						Text(color.prefix(1).uppercased() + color.dropFirst())
							.foregroundColor(.white)
							.font(.caption)
							.frame(maxWidth: .infinity)
							.padding(.vertical, 10)
							.background(stroopColor(color))
							.cornerRadius(8)
					}
					.buttonStyle(PlainButtonStyle())
				}
			}
			.padding(.horizontal, 4)
		}
		.padding(.vertical, 8)
	}

	private func setupTrials() {
		let rawColors = viewModel.input.stroopColors
		let parsed = rawColors.split(separator: ",")
			.map { $0.trimmingCharacters(in: .whitespaces) }
			.filter { !$0.isEmpty }
		colors = parsed.isEmpty ? ["red", "blue", "green", "yellow"] : parsed

		let numTrials = Int(viewModel.input.stroopTrials)
		var generated: [StroopTrialData] = []
		for _ in 0..<numTrials {
			guard let inkColor = colors.randomElement() else { continue }
			let word = viewModel.input.stroopShowWord ? (colors.randomElement() ?? inkColor) : inkColor
			generated.append(StroopTrialData(word: word, inkColor: inkColor))
		}
		trials = generated
		trialStartTime = Date()
	}

	private func stroopColor(_ name: String) -> Color {
		switch name.lowercased() {
		case "red":    return Color(red: 0.898, green: 0.224, blue: 0.208)
		case "blue":   return Color(red: 0.118, green: 0.533, blue: 0.898)
		case "green":  return Color(red: 0.263, green: 0.627, blue: 0.278)
		case "yellow": return Color(red: 0.992, green: 0.847, blue: 0.208)
		case "purple": return Color(red: 0.557, green: 0.141, blue: 0.667)
		case "orange": return Color(red: 0.984, green: 0.549, blue: 0.0)
		case "pink":   return Color(red: 0.914, green: 0.118, blue: 0.388)
		case "cyan":   return Color(red: 0.0,   green: 0.675, blue: 0.757)
		default:       return Color.gray
		}
	}
}
