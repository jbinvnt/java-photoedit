import java.util.*;
import java.io.*;

import javax.imageio.*;

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.shape.*;
import javafx.geometry.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.effect.*;

import java.awt.image.*;

public class Main extends Application{
	private static int toolbarHeight;
	private static final int TOOL_WIDTH = 100;
	
	private static Color backColor = Color.rgb(255, 255, 255);
	private static Scanner consoleInput = new Scanner(System.in);
	private static Image origImg;
	private static double canWidth;
	private static double canHeight;
	private ArrayList<ToggleButton> tools = new ArrayList<ToggleButton>();
	private static FlowPane toolbar;
	private ToggleGroup tg = new ToggleGroup();
	
	private Group root;
	private Scene scene;
	private ImageView viewer;
	private Slider glowSlider;
	private Slider sepiaSlider;
	private ArrayList<ArrayList<Rectangle>> strokes = new ArrayList<ArrayList<Rectangle>>(); //keeps a record of all the brush strokes on the image
	private ArrayList<ArrayList<Rectangle>> undoneStrokes = new ArrayList<ArrayList<Rectangle>>(); //keeps a record of everything that got undone
	private ArrayList<Rectangle> currentStroke;
	
	public void start(Stage stage){
		root = new Group();
		//File Chooser Initialization
		FileChooser fc = new FileChooser();
		fc.setTitle("Open Image");
		fc.setInitialDirectory(new File("src"));
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp")); //only allows these types to be opened
		File imgFile = fc.showOpenDialog(stage);
		if(imgFile == null){
			System.exit(0);
		}
		origImg = new Image(imgFile.toURI().toString(), canWidth, canHeight, false, false);
		//Image initialization
		viewer = new ImageView();
		HBox iBox = new HBox();
        viewer.setImage(origImg);
        iBox.getChildren().add(viewer);
        root.getChildren().add(iBox);
        //Scene initialization
		scene = new Scene(root, canWidth, canHeight+toolbarHeight, backColor);
		//Toolbar initialization
		toolbar = new FlowPane(10, 10); //sets the vertical and horizontal gap as 10 pixels
        toolbar.setOnMouseEntered(new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent e){
        		scene.setCursor(Cursor.DEFAULT); //change the mouse cursor to normal when it enters the toolbar at the bottom
        	}
        });
        toolbar.setOnMouseExited(new EventHandler<MouseEvent>(){
        	@Override
        	public void handle(MouseEvent e){
        		scene.setCursor(Cursor.CROSSHAIR); //changes the cursor back to a crosshair when leaving the toolbar
        	}
        });
		toolbar.setLayoutX(0);
        toolbar.setLayoutY(canHeight);
        toolbar.setPrefHeight(toolbarHeight);
        toolbar.setPrefWrapLength(canWidth);
        root.getChildren().add(toolbar);
        //Brush initialization
        createTool("Eraser", false, "blue");
        createTool("Brush 1", true, "yellow");
        createTool("Brush 2", true, "green");
        createTool("Brush 3", true, "orange");
        createTool("Brush 4", true, "purple");
        //Glow Filter Initialization
        Button glowFilter = new Button("Glow Filter");
        glowFilter.setMinWidth(TOOL_WIDTH);
        glowFilter.setMaxWidth(TOOL_WIDTH);
        glowFilter.setStyle("-fx-color: chartreuse");
        toolbar.getChildren().add(glowFilter);
        glowSlider = new Slider();
        glowSlider.setMin(0);
        glowSlider.setMax(1);
        glowSlider.setShowTickLabels(true);
        glowSlider.setShowTickMarks(true);
        glowSlider.setMajorTickUnit(0.1);
        glowSlider.setMinorTickCount(10);
        glowSlider.setBlockIncrement(0.1);
        glowSlider.setValue(0.5);
        glowSlider.setStyle("-fx-color: chartreuse");
        toolbar.getChildren().add(glowSlider);
        glowFilter.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event){
        		viewer.setEffect(new Glow(glowSlider.getValue()));
        	}
        });
        //Sepia Filter Initialization
        Button sepiaFilter = new Button("Sepia Filter");
        sepiaFilter.setMinWidth(TOOL_WIDTH);
        sepiaFilter.setMaxWidth(TOOL_WIDTH);
        sepiaFilter.setStyle("-fx-color: brown");
        toolbar.getChildren().add(sepiaFilter);
        sepiaSlider = new Slider();
        sepiaSlider.setMin(0);
        sepiaSlider.setMax(1);
        sepiaSlider.setShowTickLabels(true);
        sepiaSlider.setShowTickMarks(true);
        sepiaSlider.setMajorTickUnit(0.1);
        sepiaSlider.setMinorTickCount(10);
        sepiaSlider.setBlockIncrement(0.1);
        sepiaSlider.setValue(0.5);
        sepiaSlider.setStyle("-fx-color: brown");
        toolbar.getChildren().add(sepiaSlider);
        sepiaFilter.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event){
        		viewer.setEffect(new SepiaTone(sepiaSlider.getValue()));
        	}
        });
        //Save button initialization
        Button saveButton = new Button("Save Canvas");
        saveButton.setMinWidth(TOOL_WIDTH);
        saveButton.setMaxWidth(TOOL_WIDTH);
        saveButton.setStyle("-fx-color: teal");
        toolbar.getChildren().add(saveButton);
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event){
        		saveCanvas(root);
        	}
        });
        //Undo Initialization
      	Button undoButton = new Button("Undo");
        undoButton.setMinWidth(TOOL_WIDTH);
        undoButton.setMaxWidth(TOOL_WIDTH);
        undoButton.setStyle("-fx-color: red");
        toolbar.getChildren().add(undoButton);
        undoButton.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event){
        		undo();
        	}
        });
      //Redo Initialization
      	Button redoButton = new Button("Redo");
        redoButton.setMinWidth(TOOL_WIDTH);
        redoButton.setMaxWidth(TOOL_WIDTH);
        redoButton.setStyle("-fx-color: cyan");
        toolbar.getChildren().add(redoButton);
        redoButton.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event){
        		redo();
        	}
        });
		//Mouse initialization
		scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event){
				paint(event.getSceneX(), event.getSceneY());
			}
		});
		scene.setOnMousePressed(new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event){
				if(event.getSceneX() < canWidth && event.getSceneY() < canHeight){
					newStroke();
					paint(event.getSceneX(), event.getSceneY()); //this line is repeated here because it allows the user to draw a single dot by clicking
				}
			}
		});
		//Stage Initialization
		stage.setTitle("Graphic Design");
		stage.setScene(scene);
        stage.show();
	}
	
	public static void main(String[] args){
		boolean openFailed = false;
		System.out.println("Java Image Editor - 2018");
		System.out.println("Written by Joseph Black for Advanced Topics in Computer Science H");
		System.out.println("Version 1.0.0");
		System.out.print("Enter the canvas width in pixels: ");
		canWidth = Double.parseDouble(consoleInput.nextLine());
		System.out.print("Enter the canvas height in pixels: ");
		toolbarHeight = (int)(Math.pow(10, 5.25)/canWidth);
		canHeight = Double.parseDouble(consoleInput.nextLine());
		launch(args);
	}
	public ToggleButton createTool(String toolName, boolean colorPicker, String toolColor){
		ToggleButton tool = new ToggleButton(toolName);
		String css = "-fx-color: " + toolColor + ";";
		tool.setMinWidth(TOOL_WIDTH);
		tool.setMaxWidth(TOOL_WIDTH);
		tool.setUserData(toolName);
		tool.setStyle(css);
		tool.setToggleGroup(tg);
		toolbar.getChildren().add(tool);
		if(colorPicker){
			final ColorPicker cp = new ColorPicker();
			cp.setValue(backColor); //start the colorpicker at the background value for the image
			cp.setStyle(css);
			toolbar.getChildren().add(cp);
		}
		Slider slider = new Slider();
		slider.setMin(0);
		slider.setMax(100);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit(50);
		slider.setMinorTickCount(5);
		slider.setBlockIncrement(1);
		slider.setValue(5); //default value for all brushes
		slider.setStyle(css);
		toolbar.getChildren().add(slider);
		CheckBox check = new CheckBox("Rounded");
		check.setStyle(css);
		toolbar.getChildren().add(check);
		return tool;
	}
	public static boolean getYesNo(String message){
		System.out.print(message);
		String response = consoleInput.nextLine();
		return response.equals("Y") || response.equals("y");
	}
	public void paint(double x, double y){
		clearRedo();
		if(tg.getSelectedToggle() != null){ //prevents an error when no tool is selected
			Color currentColor;
			int currentBrushIndex = toolbar.getChildren().indexOf(tg.getSelectedToggle());
			if(tg.getSelectedToggle().getUserData().equals("Eraser")){
				currentColor = this.backColor; //the color of the eraser is the background color
				currentBrushIndex--; //the eraser doesn't have a color picker so this is needed to make sure the other tools can get selected
			}
			else{
				ColorPicker currentPicker = (ColorPicker)toolbar.getChildren().get(currentBrushIndex+1); //in the case of a brush, the color picker is the next tool
				currentColor = currentPicker.getValue();
			}
			Slider currentSlider = (Slider)toolbar.getChildren().get(currentBrushIndex+2); //the slider is the next tool after the colorpicker or tool button
			CheckBox currentCheckbox = (CheckBox)(toolbar.getChildren().get(currentBrushIndex+3)); //the rounded checkbox is right after the slider
			boolean rounded = currentCheckbox.isSelected();
			int currentSize = (int)(currentSlider.getValue());
			if(x > 0 && x < canWidth-currentSize/2 && y > 0 && y < canHeight-currentSize/2){ //only draw if cursor is within window
				Rectangle dot = new Rectangle(x-currentSize/2, y-currentSize/2, currentSize, currentSize);
				if(rounded){ //makes dots a circle if rounded is selected
					dot.setArcWidth(currentSize);
					dot.setArcHeight(currentSize);
				}
				dot.setFill(currentColor);
				currentStroke.add(dot);
				root.getChildren().add(dot);
			}
		}
	}
	public void newStroke(){
		currentStroke = new ArrayList<Rectangle>();
		strokes.add(currentStroke);
	}
	public void saveCanvas(Group group){
		SnapshotParameters params = new SnapshotParameters();
		params.setViewport(new Rectangle2D(0, 0, this.canWidth, this.canHeight)); //sets the captured area to exclude the toolbar
		Image snapshot = group.snapshot(params, null);
		RenderedImage rendered = SwingFXUtils.fromFXImage(snapshot, null);
		FileChooser saveChooser = new FileChooser();
		saveChooser.setTitle("Save Canvas");
		saveChooser.setInitialDirectory(new File("src"));
		saveChooser.setInitialFileName("MyImage.png");
		saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png")); //only can save the image as a PNG
		File saveFile = saveChooser.showSaveDialog(null);
		try{
			ImageIO.write(rendered, "png", saveFile);
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	public void undo(){
		if(strokes.size() >= 1){
			int sIndex = strokes.size() - 1;
			root.getChildren().removeAll(strokes.get(sIndex));
			undoneStrokes.add(strokes.get(sIndex));
			strokes.remove(sIndex);
		}
	}
	public void redo(){
		if(undoneStrokes.size() >= 1){
			int sIndex = undoneStrokes.size() - 1;
			root.getChildren().addAll(undoneStrokes.get(sIndex));
			strokes.add(undoneStrokes.get(sIndex));
			undoneStrokes.remove(sIndex);
		}
	}
	public void clearRedo(){
		undoneStrokes = new ArrayList<ArrayList<Rectangle>>(); //when a new stroke is drawn, anything saved in the redo queue will be cleared
	}
}
