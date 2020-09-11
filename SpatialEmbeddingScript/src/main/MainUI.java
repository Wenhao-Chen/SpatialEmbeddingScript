package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class MainUI extends JPanel implements ActionListener{
	
	JPanel left, top, center;
	
	JTextField tf_outPath, tf_rateMin, tf_rateMax, tf_rateInc;
	
	JFileChooser fc;
	
	DefaultListModel<File> models;
	
	private Set<String> selectedApps = new HashSet<>();
	private Set<File> selectedInputImages = new HashSet<>();
	
	
	MainUI() {
		setLayout(new GridLayout(0, 1));
		initLeftPanel();
		
		JPanel right = new JPanel(new BorderLayout());
		initTopPanel();
		right.add(top, BorderLayout.NORTH);
		initCenterPanel();
		right.add(center, BorderLayout.CENTER);
		
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		sp.setDividerLocation(200);
		add(sp);
		
		fc = new JFileChooser();
		fc.setCurrentDirectory(new File("."));
	}
	
	private void initLeftPanel() {
		left = new JPanel(new BorderLayout());
		
		JButton b_input = new JButton("Add Input Images");
		b_input.setActionCommand("input");
		b_input.addActionListener(this);
		//left.add(b_input, BorderLayout.NORTH);
		
		JLabel l_numInput = new JLabel("num input: 0");
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.add(b_input);
		p.add(l_numInput);
		l_numInput.setAlignmentX(CENTER_ALIGNMENT);
		left.add(p, BorderLayout.NORTH);
		
		models = new DefaultListModel<File>();
		JList<File> jl = new JList<>(models);
		JScrollPane jlsp = new JScrollPane(jl);
		left.add(jlsp, BorderLayout.CENTER);
		
	}
	
	private void initTopPanel() {
		top = new JPanel(new GridLayout(0, 1));
		top.setBorder(BorderFactory.createTitledBorder("Options"));
		
		JPanel row1 = new JPanel(new BorderLayout(20, 0));
		row1.add(new JLabel("Output dir: "), BorderLayout.WEST);
		tf_outPath = new JTextField();
		tf_outPath.setFont(new Font("Arial", Font.PLAIN, 15));
		row1.add(tf_outPath, BorderLayout.CENTER);
		JButton b_browseOutput = new JButton("Browse");
		b_browseOutput.setActionCommand("browse");
		b_browseOutput.addActionListener(this);
		row1.add(b_browseOutput, BorderLayout.EAST);
		top.add(row1);
		
		JPanel row_apps = new JPanel(new GridLayout(0,4));
		row_apps.setBorder(BorderFactory.createTitledBorder("Stego Apps"));
		for (int i=0; i<ScriptMain.AllApps.size(); i++) {
			String appName = ScriptMain.AllApps.get(i).getSimpleName();
			JCheckBox cb_app = new JCheckBox(appName);
			cb_app.setSelected(true);
			cb_app.setActionCommand("toggle_app "+appName);
			cb_app.addActionListener(this);
			row_apps.add(cb_app);
		}
		top.add(row_apps);
		
		JPanel row_rates = new JPanel(new FlowLayout(FlowLayout.LEFT));
		row_rates.add(new JLabel("Embedding Rates: from "));
		tf_rateMin = new JTextField("0.05");
		row_rates.add(tf_rateMin);
		row_rates.add(new JLabel(" to "));
		tf_rateMax = new JTextField("0.25");
		row_rates.add(tf_rateMax);
		row_rates.add(new JLabel(" with interval "));
		tf_rateInc = new JTextField("0.05");
		row_rates.add(tf_rateInc);
		
		JPanel row_validate = new JPanel(new FlowLayout(FlowLayout.LEFT));
		row_validate.add(new JLabel("Validate stego images after creation? (could be slow)"));
		JRadioButton rb_Yes = new JRadioButton("Yes");
		rb_Yes.setActionCommand("validate:yes");
		rb_Yes.addActionListener(this);
		JRadioButton rb_No = new JRadioButton("No");
		rb_No.setActionCommand("validate:no");
		rb_No.addActionListener(this);
		ButtonGroup bg = new ButtonGroup();
		bg.add(rb_Yes); bg.add(rb_No);
		rb_Yes.setSelected(false); rb_No.setSelected(true);
		row_validate.add(rb_Yes);
		row_validate.add(rb_No);
		
		JCheckBox cb_validate = new JCheckBox();
		cb_validate.setActionCommand("validate");
		cb_validate.addActionListener(this);
		
		JPanel twoRows = new JPanel(new GridLayout(0,1));
		twoRows.add(row_rates);
		twoRows.add(row_validate);
		top.add(twoRows);
	}
	
	private void initCenterPanel() {
		center = new JPanel();
		
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		tf_outPath.setText(command);
		if (command.equals("browse")) {
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				tf_outPath.setText(fc.getSelectedFile().getAbsolutePath());
			}
		}
		else if (command.equals("input")) {
			fc.setMultiSelectionEnabled(true);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File[] files = fc.getSelectedFiles();
				for (File f : files) {
					List<File> images = ScriptMain.collectInputImages(f.getAbsolutePath());
					for (File image : images)
					if (selectedInputImages.add(image)) {
						models.addElement(image);
					}
						
				}
			}
		}
		else if (command.startsWith("toggle_app")) {
			String appName = command.substring(command.indexOf(" ")+1);
			if (selectedApps.contains(appName))
				selectedApps.remove(appName);
			else
				selectedApps.add(appName);
		}
		else if (command.equals("validate:yes")) {
			ScriptMain.validate = true;
		}
		else if (command.equals("validate:no")) {
			ScriptMain.validate = false;
		}
	}
	

	//public static void main(String[] args) { createAndShow(); }
	
	public static void createAndShow() {
		String title = "Stego Embedding Script";
		JPanel panel = new MainUI();
		JFrame frame = new JFrame(title);

		int minWidth = 800;
		int minHeight = 600;
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int left = (screenSize.width-minWidth)/2;
        int top = (screenSize.height-minHeight)/2;
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocation(left, top);
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.pack();
	}

	
}
