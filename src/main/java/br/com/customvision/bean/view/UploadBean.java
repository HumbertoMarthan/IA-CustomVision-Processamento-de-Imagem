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

	/*
	 * Faz o Upload da Foto para a pasta de escolha... No caso a pasta Test dentro
	 * do projeto
	 */
	public void upload() {
		try {
			String caminho = "C:\\Users\\Humberto\\workspace-tcc\\cognitive-services-java-sdk-samples\\Vision\\CustomVision\\src\\main\\resources\\Test";

			File file = new File(caminho, uploadedFile.getFileName());

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

	/* Retorna o local e nome da imagem a ser pego */
	private static byte[] GetImage(String folder, String filename) {
		try {
			return ByteStreams.toByteArray(CustomVisionSamples.class.getResourceAsStream(folder + "/" + filename));
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/* Adiciona as Imagens ao treinamento */
	private void AddImageToProject(Trainings trainer, Project project, String fileName, byte[] contents, UUID tag,
			double[] regionValues) {
		System.out.println("Adicionando imagem: " + fileName);
		ImageFileCreateEntry file = new ImageFileCreateEntry().withName(fileName).withContents(contents);

		ImageFileCreateBatch batch = new ImageFileCreateBatch().withImages(Collections.singletonList(file));
		if (regionValues != null) {
			Region region = new Region().withTagId(tag).withLeft(regionValues[0]).withTop(regionValues[1])
					.withWidth(regionValues[2]).withHeight(regionValues[3]);
			file = file.withRegions(Collections.singletonList(region));
		} else {
			batch = batch.withTagIds(Collections.singletonList(tag));
		}

		trainer.createImagesFromFiles(project.id(), batch);
	}

	public void testar() {

		try {
			// =============================================================
			// Authenticate

			final String trainingApiKey = "d2779462048c47acb5fd112c4d394dbd";

			final String predictionApiKey = "13aaabd699d8470387f675dfb0671fc7";

			TrainingApi trainClient = CustomVisionTrainingManager.authenticate(trainingApiKey);
			PredictionEndpoint predictClient = CustomVisionPredictionManager.authenticate(predictionApiKey);
			// =============================================================

			System.out.println("Classificação de Cor de olhos");
			Trainings trainer = trainClient.trainings();

			/*
			 * String name = "7d190085-1232-44d6-80e3-3bb85945315f"; String source = name;
			 * byte[] bytes = source.getBytes("UTF-8"); UUID projeto =
			 * UUID.nameUUIDFromBytes(bytes); Project project = trainer.getProject(projeto);
			 */

			System.out.println("Projeto está sendo criado...");

			Project project = trainer.createProject().withName("Projeto Diferença de cor de olhos").execute();

			// azul tag
			Tag azulTag = trainer.createTag().withProjectId(project.id()).withName("Azul").execute();

			// castanho tag
			Tag castanhoTag = trainer.createTag().withProjectId(project.id()).withName("Castanho").execute();

			// preto Tag
			
			  Tag pretoTag = trainer.createTag() .withProjectId(project.id())
			  .withName("Preto") .execute();
			 

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

			for (int i = 1; i <= 10; i++) {
				String fileName = "preto" + i + ".jpg";
				byte[] contents = GetImage("/Preto", fileName);
				AddImageToProject(trainer, project, fileName, contents, pretoTag.id(), null);
			}

			System.out.println("Treinando Imagens Adicionadas......");
			Iteration iteration = trainer.trainProject(project.id());

			while (iteration.status().equals("Training")) {
				System.out.println("Status do Treino: " + iteration.status());

				Thread.sleep(1000);
				iteration = trainer.getIteration(project.id(), iteration.id());
			}
			System.out.println("Status do Treino:	 " + iteration.status());
			trainer.updateIteration(project.id(), iteration.id(), iteration.withIsDefault(true));

			String fileName = getUploadedFile().getFileName();

			// carrega imagem de teste
			byte[] testImage = GetImage("/Test", fileName);

			// Predição
			ImagePrediction results = predictClient.predictions().predictImage().withProjectId(project.id())
					.withImageData(testImage).execute();

			for (Prediction prediction : results.predictions()) {
				System.out.println(
						String.format("\t%s: %.2f%%", prediction.tagName(), prediction.probability() * 100.0f));
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Upload completo", "A cor do olho "
						+ prediction.tagName() + " tem probabilidade: " + prediction.probability() * 100.0f + "%"));
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
