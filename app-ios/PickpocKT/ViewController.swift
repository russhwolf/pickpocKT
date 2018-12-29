//
//  ViewController.swift
//  app-ios
//
//  Created by Russell Wolf on 7/1/18.
//  Copyright © 2018 Russell Wolf. All rights reserved.
//

import UIKit
import shared

class ViewController: UIViewController, UITableViewDelegate, UITableViewDataSource, Kotlinx_coroutines_core_nativeCoroutineScope
{
    let job: Kotlinx_coroutines_core_nativeJob = IosHelpersKt.SupervisorJob()
    lazy var coroutineContext: KotlinCoroutineContext = job.plus(context: NsQueueDispatcherKt.iosMainDispatcher)

    let viewModel = IosHelpersKt.createViewModel(defaults: UserDefaults.standard)

    @IBOutlet var guessList: UITableView?
    @IBOutlet var currentGuessView: UILabel?
    @IBOutlet var button1: UIButton?
    @IBOutlet var button2: UIButton?
    @IBOutlet var button3: UIButton?
    @IBOutlet var button4: UIButton?
    @IBOutlet var button5: UIButton?
    @IBOutlet var button6: UIButton?
    @IBOutlet var lockImage: UIImageView?
    @IBOutlet var startLocalButton: UIButton?
    @IBOutlet var startWebButton: UIButton?
    @IBOutlet var resetButton: UIButton?
    
    lazy var buttons = [button1, button2, button3, button4, button5, button6]
    
    var inputDialog: UIAlertController? = nil
    var usersDialog: UIAlertController? = nil
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        guessList?.register(UINib(nibName: "GuessListCell", bundle: nil), forCellReuseIdentifier: "cell")
        guessList?.delegate = self
        guessList?.dataSource = self

        viewModel.setListener(listener: { (state: ViewState) -> KotlinUnit in
            self.resetButton?.alpha = state.resetButtonVisible ? 1 : 0
            self.startLocalButton?.alpha = state.startButtonsVisible ? 1 : 0
            self.startWebButton?.alpha = state.startButtonsVisible ? 1 : 0
            for button in self.buttons {
                button?.isEnabled = state.enabled
            }
            self.currentGuessView?.text = state.guess
            self.setItems(items: state.results)
            
            let imageName = state.locked ? "LockClosed" : "LockOpen"
            self.lockImage?.image = UIImage.init(named: imageName)
            
            if (state.localConfigVisible) {
                if (self.inputDialog == nil) {
                    self.showInputDialog()
                }
            } else {
                self.inputDialog?.dismiss(animated: true, completion: nil)
                self.inputDialog = nil
            }
            
            if (state.webConfigOptions != nil) {
                if (self.usersDialog == nil) {
                    self.showUsersDialog(users: state.webConfigOptions!)
                }
            } else {
                self.usersDialog?.dismiss(animated: true, completion: nil)
                self.usersDialog = nil
            }
            
            return KotlinUnit()
        })
    }

    deinit {
        viewModel.deinit()
        job.cancel()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func pressStartLocal() {
        viewModel.startLocal()
    }
    
    @IBAction func pressStartWeb() {
        IosHelpersKt.launchStartWeb(self, lockViewModel: viewModel)
    }
    
    @IBAction func pressReset() {
        viewModel.reset()
    }
    
    @IBAction func pressButton1() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "1")
    }
    
    @IBAction func pressButton2() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "2")
    }
    
    @IBAction func pressButton3() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "3")
    }
    
    @IBAction func pressButton4() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "4")
    }
    
    @IBAction func pressButton5() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "5")
    }
    
    @IBAction func pressButton6() {
        IosHelpersKt.launchInput(self, lockViewModel: viewModel, character: "6")
    }
    
    var items: [GuessListItem] = []
    
    func setItems(items: [GuessListItem]) {
        self.items = items
        self.guessList?.reloadData()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! GuessListCell
        cell.numCorrectView?.text = String(items[indexPath.row].numCorrect)
        cell.numMisplacedView?.text = String(items[indexPath.row].numMisplaced)
        cell.guessView?.text = items[indexPath.row].guess
        
        return cell
    }
    
    private func showInputDialog() {
        let inputDialog = UIAlertController(title: "Select Code Length", message: nil, preferredStyle: .alert)
        
        inputDialog.addTextField { (textField) in
            textField.keyboardType = UIKeyboardType.numberPad
        }
        inputDialog.addAction(UIAlertAction(title: "OK", style: .default) { (_) in
            let codeLength = inputDialog.textFields?[0].text ?? ""
            self.viewModel.selectLocalLength(codeLengthInput: codeLength)
        })
        inputDialog.addAction(UIAlertAction(title: "Cancel", style: .cancel) { (_) in
            self.viewModel.dismissLocalLengthInput()
        })

        self.inputDialog = inputDialog
        present(inputDialog, animated: true, completion: nil)
    }
    
    private func showUsersDialog(users: [WebLockProvider.User]) {
        let usersDialog = UIAlertController(title: "Select Lock to Pick", message: nil, preferredStyle: .actionSheet)
        
        for user in users {
            usersDialog.addAction(UIAlertAction(title: "\(user.name) (length \(user.codeLength))", style: .default) { (action) in
                self.viewModel.selectWebUser(user: user)
            })
        }
        usersDialog.addAction(UIAlertAction(title: "Cancel", style: .cancel) { (_) in
            self.viewModel.dismissWebUserInput()
        })
        
        self.usersDialog = usersDialog
        present(usersDialog, animated: true, completion: nil)
    }
}

class GuessListCell : UITableViewCell {
    @IBOutlet var numCorrectView: UILabel?
    @IBOutlet var numMisplacedView: UILabel?
    @IBOutlet var guessView: UILabel?
}
