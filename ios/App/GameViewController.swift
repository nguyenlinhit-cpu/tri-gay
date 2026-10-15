import UIKit

final class GameViewController: UIViewController {
    private var gameView: NativeGameView?
    override var prefersStatusBarHidden: Bool { true }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .landscape }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override func loadView() {
        do {
            guard let url = Bundle.main.url(forResource: "levels", withExtension: "json", subdirectory: "Resources") else {
                throw NSError(domain: "SupperMario", code: 1, userInfo: [NSLocalizedDescriptionKey: "Không tìm thấy dữ liệu màn chơi."])
            }
            let engine = GameEngine(library: try LevelLibrary.decode(Data(contentsOf: url)))
            #if DEBUG
            if let argument = ProcessInfo.processInfo.arguments.firstIndex(of: "--smoke-level"),
               ProcessInfo.processInfo.arguments.count > argument + 1,
               let index = Int(ProcessInfo.processInfo.arguments[argument + 1]) {
                engine.start(at: index - 1)
            }
            #endif
            let game = NativeGameView(engine: engine)
            gameView = game
            view = game
        } catch {
            let label = UILabel()
            label.backgroundColor = .black; label.textColor = .white
            label.textAlignment = .center; label.numberOfLines = 0
            label.text = "Không thể mở game: \(error.localizedDescription)"
            view = label
        }
    }
    override func viewDidAppear(_ animated: Bool) { super.viewDidAppear(animated); gameView?.startDisplayLink() }
    override func viewWillDisappear(_ animated: Bool) { gameView?.stopDisplayLink(); super.viewWillDisappear(animated) }
}
