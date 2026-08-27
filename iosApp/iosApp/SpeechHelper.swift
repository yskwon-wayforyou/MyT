import Speech
import AVFoundation

/// Native iOS speech helper (call from SwiftUI / future Kotlin bridge).
enum SpeechHelper {
    static func requestAuth(completion: @escaping (Bool) -> Void) {
        SFSpeechRecognizer.requestAuthorization { status in
            DispatchQueue.main.async {
                completion(status == .authorized)
            }
        }
    }

    static func recognize(
        locale: String = "ko-KR",
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        requestAuth { ok in
            guard ok else {
                completion(.failure(NSError(domain: "MyTSpeech", code: 1, userInfo: [
                    NSLocalizedDescriptionKey: "음성 인식 권한이 없습니다",
                ])))
                return
            }
            guard let recognizer = SFSpeechRecognizer(locale: Locale(identifier: locale)),
                  recognizer.isAvailable else {
                completion(.failure(NSError(domain: "MyTSpeech", code: 2, userInfo: [
                    NSLocalizedDescriptionKey: "음성 인식을 사용할 수 없습니다",
                ])))
                return
            }

            let session = AVAudioSession.sharedInstance()
            do {
                try session.setCategory(.record, mode: .measurement, options: .duckOthers)
                try session.setActive(true, options: .notifyOthersOnDeactivation)
            } catch {
                completion(.failure(error))
                return
            }

            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = false
            let engine = AVAudioEngine()
            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                request.append(buffer)
            }
            engine.prepare()
            do { try engine.start() } catch {
                completion(.failure(error))
                return
            }

            recognizer.recognitionTask(with: request) { result, error in
                if let error {
                    engine.stop()
                    input.removeTap(onBus: 0)
                    completion(.failure(error))
                    return
                }
                if let result, result.isFinal {
                    engine.stop()
                    input.removeTap(onBus: 0)
                    request.endAudio()
                    let text = result.bestTranscription.formattedString
                    if text.isEmpty {
                        completion(.failure(NSError(domain: "MyTSpeech", code: 3, userInfo: [
                            NSLocalizedDescriptionKey: "인식된 음성이 없습니다",
                        ])))
                    } else {
                        completion(.success(text))
                    }
                }
            }
        }
    }
}
