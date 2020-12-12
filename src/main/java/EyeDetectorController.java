import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.scene.control.Alert;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class EyeDetectorController
{
	@FXML
	private Button cameraButton;
	@FXML
	private ImageView originalFrame;
	private ScheduledExecutorService timer;
	private VideoCapture vidCapture;
	private boolean cameraActive;
	CascadeClassifier eyeCascade;
	CascadeClassifier faceCascade;

	protected void init()
	{
		this.vidCapture = new VideoCapture();
		eyeCascade=new CascadeClassifier();
		faceCascade=new CascadeClassifier();
		eyeCascade.load("src/main/java/haarcascade_eye.xml");
		faceCascade.load("src/main/java/haarcascade_frontalface_alt.xml");
		originalFrame.setFitWidth(600);
		originalFrame.setPreserveRatio(true);
	}

	private void showAlert(String title,String text ) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(text);
		alert.showAndWait();
	}

	@FXML
	protected void startCamera()
	{	
		if (!this.cameraActive)
		{
			this.vidCapture.open(0);

			if (this.vidCapture.isOpened())
			{
				this.cameraActive = true;

				Runnable frameGrabber = new Runnable() {
					
					//@Override
					public void run()
					{
						Mat frame = grabFrame();
						Image imageToShow = ImageCapture.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				this.cameraButton.setText("Остановить");
			}
			else
			{
				System.err.println("Фэйл. Камеры не обнаружено.");
				showAlert("Ошибка", "Камер не обнаружено.");
			}
		}
		else
		{
			this.cameraActive = false;
			this.cameraButton.setText("Начать");
			this.stop();
		}
	}

	private Mat grabFrame()
	{
		Mat frame = new Mat();
		if (this.vidCapture.isOpened())
		{
			try
			{
				this.vidCapture.read(frame);
				if (!frame.empty())
				{
					this.detectAndDisplay(frame);
				}
			}
			catch (Exception e)
			{
				System.err.println("Исключение во время обработки изображения: " + e);
			}
		}
		
		return frame;
	}

	private void detectAndDisplay(Mat frame)
	{
		MatOfRect faces = new MatOfRect();

		MatOfRect eyes = new MatOfRect();
		Mat grayFrame = new Mat();
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		faceCascade.detectMultiScale(grayFrame,faces,1.1,4, 0, new Size(20, 20), new Size());
		Rect[] faceArray = faces.toArray();
		for (int i = 0; i < faceArray.length; i++) {
			Imgproc.rectangle(frame, new Point(faceArray[i].x, faceArray[i].y), new Point(faceArray[i].x + faceArray[i].width, faceArray[i].y + faceArray[i].height), new Scalar(0, 255, 255), 1);
			Mat grayFaceFrame = grayFrame;
			Rect ROI = new Rect(faceArray[i].x,faceArray[i].y,faceArray[0].width/2,faceArray[0].height/2 );
			Imgproc.rectangle(frame,new Point(faceArray[i].x, faceArray[i].y), new Point(faceArray[i].x + faceArray[i].width/2, faceArray[i].y + faceArray[i].height/2),new Scalar(0,0,255),1);
			grayFaceFrame = grayFaceFrame.submat(ROI);
			eyeCascade.detectMultiScale(grayFaceFrame,eyes,1.1,4, 0, new Size(10, 10), new Size());
			Rect[] eyesArray = eyes.toArray();
			for(int j=0;j<eyesArray.length;j++)
			{
				Imgproc.rectangle(frame, new Point(eyesArray[j].x+ROI.x, eyesArray[j].y+ROI.y),
						new Point(eyesArray[j].x+ROI.x + eyesArray[j].width, eyesArray[j].y+ROI.y + eyesArray[j].height) ,
						new Scalar(0, 255, 0), 2);
			}
		}
	}

	private void stop()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				showAlert("Ошибка", "Остановка съемки.");
				System.err.println("Остановка съемки, попытка освободить камеру... " + e);
			}
		}
		
		if (this.vidCapture.isOpened())
		{
			this.vidCapture.release();
		}
	}

	private void updateImageView(ImageView view, Image image)
	{
		ImageCapture.onFXThread(view.imageProperty(), image);
	}

	protected void setClosed()
	{
		this.stop();
	}
	
}
