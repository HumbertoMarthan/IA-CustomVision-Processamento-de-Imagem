package br.com.customvision.bean.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.primefaces.model.UploadedFile;

import com.google.common.io.ByteStreams;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.CustomVisionPredictionManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.PredictionEndpoint;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.ImagePrediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.prediction.models.Prediction;
import com.microsoft.azure.cognitiveservices.vision.customvision.samples.CustomVisionSamples;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.CustomVisionTrainingManager;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.TrainingApi;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.Trainings;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateBatch;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.ImageFileCreateEntry;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Iteration;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Project;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Region;
import com.microsoft.azure.cognitiveservices.vision.customvision.training.models.Tag;

@ManagedBean
public class UploadBean {

	private UploadedFile uploadedFile;

	public void upload() {
		try {
			File file = new File(
					"C:\\Users\\Humberto\\workspace-tcc\\cognitive-services-java-sdk-samples\\Vision\\CustomVision\\src\\main\\resources\\Test",
					uploadedFile.getFileName());

			OutputStream out = new FileOutputStream(file);
			out.write(uploadedFile.getContents());
			out.close();

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage("Upload completo", "O arquivo " + uploadedFile.getFileName() + " foi salvo!"));
		} catch (IOException e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Erro", e.getMessage()));
		}

	}

	private static byte[] GetImage(String folder, String fileName) {
		try {
			return ByteStreams.toByteArray(CustomVisionSamples.class.getResourceAsStream(folder + "/" + fileName));
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private void AddImageToProject(Trainings trainer, Project project, String fileName, byte[] contents, UUID tag,
			double[] regionValues) {
		System.out.println("Adding image: " + fileName);
		ImageFileCreateEntry file = new ImageFileCreateEntry().withName(fileName).withContents(contents);

		ImageFileCreateBatch batch = new ImageFileCreateBatch().withImages(Collections.singletonList(file));

		// If Optional region is specified, tack it on and place the tag there,
		// otherwise
		// add it to the batch.
		if (regionValues != null) {
			Region region = new Region().withTagId(tag).withLeft(regionValues[0]).withTop(regionValues[1])
					.withWidth(regionValues[2]).withHeight(regionValues[3]);
			file = file.withRegions(Collections.singletonList(region));
		} else {
			batch = batch.withTagIds(Collections.singletonList(tag));
		}

		trainer.createImagesFromFiles(project.id(), batch);
	}



	public void runSample() {
		
			try {
				// =============================================================
				// Authenticate

				final String trainingApiKey = "d2779462048c47acb5fd112c4d394dbd";
				
				final String predictionApiKey = "13aaabd699d8470387f675dfb0671fc7";
				

				TrainingApi trainClient = CustomVisionTrainingManager.authenticate(trainingApiKey);
				PredictionEndpoint predictClient = CustomVisionPredictionManager.authenticate(predictionApiKey);

				
				//ImageClassification_Sample(trainClient, predictClient);
				
				System.out.println("Classificação de Cor de olhos");
				Trainings trainer = trainClient.trainings();

				System.out.println("Projeto está sendo criado...");
				//Project project = trainer.getProject(e958b2c5-5785-4f01-bd83-fe2a6c5771c0);
				Project project = trainer.createProject().withName("Projeto Diferença de cor de olhos").execute();

				// create azul tag
				Tag azulTag = trainer.createTag().withProjectId(project.id()).withName("Azul").execute();

				// create castanho tag
				Tag castanhoTag = trainer.createTag().withProjectId(project.id()).withName("Castanho").execute();

				for (int i = 1; i <= 10; i++) {
					String fileName = "cast_" + i + ".jpg";
					byte[] contents = GetImage("/Castanho", fileName);
					AddImageToProject(trainer, project, fileName, contents, castanhoTag.id(), null);
				}

				for (int i = 1; i <= 10; i++) {
					String fileName = "azul_" + i + ".jpg";
					byte[] contents = GetImage("/Azul", fileName);
					AddImageToProject(trainer, project, fileName, contents, azulTag.id(), null);
				}

				System.out.println("Training...");
				Iteration iteration = trainer.trainProject(project.id());

				while (iteration.status().equals("Training")) {
					System.out.println("Training Status: " + iteration.status());
					Thread.sleep(1000);
					iteration = trainer.getIteration(project.id(), iteration.id());
				}
				System.out.println("Training Status: " + iteration.status());
				trainer.updateIteration(project.id(), iteration.id(), iteration.withIsDefault(true));

				// load test image
				byte[] testImage = GetImage("/Test", "test_olhoCastanho.jpg");

				//byte[] testImage = getUploadedFile().getContents();

				// predict
				ImagePrediction results = predictClient.predictions().predictImage().withProjectId(project.id())
						.withImageData(testImage).execute();

				for (Prediction prediction : results.predictions()) {
					System.out.println(
							String.format("\t%s: %.2f%%", prediction.tagName(), prediction.probability() * 100.0f));
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage("Upload completo", "A cor do olho "+ prediction.tagName() +" Tem probabilidade"+ prediction.probability() * 100.0f));
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
	}

	public void ImageClassification_Sample(TrainingApi trainClient, PredictionEndpoint predictor) {
		try {
			System.out.println("Classificação de Cor de olhos");
			Trainings trainer = trainClient.trainings();

			System.out.println("Projeto está sendo criado...");
			Project project = trainer.createProject().withName("Projeto Diferença de cor de olhos").execute();

			// create azul tag
			Tag azulTag = trainer.createTag().withProjectId(project.id()).withName("Azul").execute();

			// create castanho tag
			Tag castanhoTag = trainer.createTag().withProjectId(project.id()).withName("Castanho").execute();

			for (int i = 1; i <= 10; i++) {
				String fileName = "cast_" + i + ".jpg";
				byte[] contents = GetImage("/Castanho", fileName);
				AddImageToProject(trainer, project, fileName, contents, castanhoTag.id(), null);
			}

			for (int i = 1; i <= 10; i++) {
				String fileName = "azul_" + i + ".jpg";
				byte[] contents = GetImage("/Azul", fileName);
				AddImageToProject(trainer, project, fileName, contents, azulTag.id(), null);
			}

			System.out.println("Training...");
			Iteration iteration = trainer.trainProject(project.id());

			while (iteration.status().equals("Training")) {
				System.out.println("Training Status: " + iteration.status());
				Thread.sleep(1000);
				iteration = trainer.getIteration(project.id(), iteration.id());
			}
			System.out.println("Training Status: " + iteration.status());
			trainer.updateIteration(project.id(), iteration.id(), iteration.withIsDefault(true));

			// load test image
			 byte[] testImage = GetImage("/Test", "test_olhoCastanho.jpg");

			//byte[] testImage = getUploadedFile().getContents();

			// predict
			ImagePrediction results = predictor.predictions().predictImage().withProjectId(project.id())
					.withImageData(testImage).execute();

			for (Prediction prediction : results.predictions()) {
				System.out.println(
						String.format("\t%s: %.2f%%", 
								prediction.tagName(), 
								prediction.probability() * 100.0f));

			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

}
