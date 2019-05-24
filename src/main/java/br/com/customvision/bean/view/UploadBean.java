package br.com.customvision.bean.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.primefaces.model.UploadedFile;

@ManagedBean
public class UploadBean {
	
	private UploadedFile uploadedFile;
	
	public void upload() {
		  try {
		    File file = new File("C:\\Users\\Humberto\\workspace-tcc\\cognitive-services-java-sdk-samples\\Vision\\CustomVision\\src\\main\\resources\\Test", uploadedFile.getFileName());
		 
		    OutputStream out = new FileOutputStream(file);
		    out.write(uploadedFile.getContents());
		    out.close();
		 
		    FacesContext.getCurrentInstance().addMessage(
		               null, new FacesMessage("Upload completo", 
		               "O arquivo " + uploadedFile.getFileName() + " foi salvo!"));
		  } catch(IOException e) {
		    FacesContext.getCurrentInstance().addMessage(
		              null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Erro", e.getMessage()));
		  }
		 
		}
	
	
	private String diretorioRaiz() {
		
		return "resources/Test/";
	}


	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}
	
}
