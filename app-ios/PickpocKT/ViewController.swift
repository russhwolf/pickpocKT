//
//  ViewController.swift
//  app-ios
//
//  Created by Russell Wolf on 7/1/18.
//  Copyright © 2018 Russell Wolf. All rights reserved.
//

import UIKit
import Shared

class ViewController: UIViewController, UITableViewDelegate, UITableViewDataSource
{
    lazy var viewModel = SharedLockViewModel(guessableProvider: SharedLockProvider(), listener: nil)

    @IBOutlet var guessList: UITableView?
    @IBOutlet var currentGuessView: UILabel?
    @IBOutlet var button1: UIButton?
    @IBOutlet var button2: UIButton?
    @IBOutlet var button3: UIButton?
    @IBOutlet var button4: UIButton?
    @IBOutlet var button5: UIButton?
    @IBOutlet var button6: UIButton?
    
    lazy var buttons = [button1, button2, button3, button4, button5, button6]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        guessList?.register(UINib(nibName: "GuessListCell", bundle: nil), forCellReuseIdentifier: "cell")
        guessList?.delegate = self
        guessList?.dataSource = self

        viewModel.setListener(listener: { (state: SharedViewState) -> SharedStdlibUnit in
            for button in self.buttons {
                button?.isEnabled = state.enabled
            }
            self.currentGuessView?.text = state.guess
            self.setItems(items: state.results)
            
            return SharedStdlibUnit()
        })
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func pressReset() {
        viewModel.reset()
    }
    
    @IBAction func pressButton1() {
        viewModel.input(character: "1")
    }
    
    @IBAction func pressButton2() {
        viewModel.input(character: "2")
    }
    
    @IBAction func pressButton3() {
        viewModel.input(character: "3")
    }
    
    @IBAction func pressButton4() {
        viewModel.input(character: "4")
    }
    
    @IBAction func pressButton5() {
        viewModel.input(character: "5")
    }
    
    @IBAction func pressButton6() {
        viewModel.input(character: "6")
    }
    
    
    var items: [SharedGuessListItem] = []
    
    func setItems(items: [SharedGuessListItem]) {
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
}

class GuessListCell : UITableViewCell {
    @IBOutlet var numCorrectView: UILabel?
    @IBOutlet var numMisplacedView: UILabel?
    @IBOutlet var guessView: UILabel?
}
