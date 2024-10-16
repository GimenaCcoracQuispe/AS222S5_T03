package pe.edu.vallegrande.traductor.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import pe.edu.vallegrande.traductor.model.TranslateRequestBody;
import pe.edu.vallegrande.traductor.model.Traslator;
import pe.edu.vallegrande.traductor.respository.TraslatorRepository;
import pe.edu.vallegrande.traductor.service.TraductorService;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@CrossOrigin(origins = "http://localhost:5153")
@RequestMapping("/api")
public class traductorRest {

	private final TraductorService traductorService;
	private final TraslatorRepository traslatorRepository;

	@Autowired
	public traductorRest(TraductorService traductorService,
			TraslatorRepository traslatorRepository) {
		this.traductorService = traductorService;
		this.traslatorRepository = traslatorRepository;
	}

	@GetMapping("/translations")
	public Mono<ResponseEntity<List<Traslator>>> getAllTranslations() {
		return traslatorRepository.findAll()
				.collectList()
				.map(translations -> {
					if (translations.isEmpty()) {
						return ResponseEntity.status(HttpStatus.NO_CONTENT).body(translations);
					}
					return ResponseEntity.status(HttpStatus.OK).body(translations);
				})
				.onErrorResume(error -> {
					log.error("Error retrieving translations: {}", error.getMessage());
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
				});
	}

	@PostMapping
	public Mono<ResponseEntity<String>> translateText(@RequestBody TranslateRequestBody requestBody) {
		String text = requestBody.getText();
		String from = requestBody.getFrom();
		String to = requestBody.getTo();

		return traductorService.translateText(text, from, to).flatMap(translatedText -> {
			Traslator traslator = new Traslator();
			traslator.setInput_text(text);
			traslator.setTranslated_text(translatedText);
			traslator.setFrom_language(from);
			traslator.setTo_language(to);
			traslator.setState("A"); // Cambiar status por state

			return traslatorRepository.save(traslator)
					.map(savedTranslation -> ResponseEntity.status(HttpStatus.OK)
							.body("Translation saved successfully"));
		}).onErrorResume(error -> {
			log.error("Error translating text: {}", error.getMessage());
			return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error translating text"));
		});
	}

	@DeleteMapping("/delete/{id}")
	public Mono<ResponseEntity<String>> deleteTranslation(@PathVariable Long id) {
		return traslatorRepository.findById(id) 
				.flatMap(traslator -> {
					traslator.setState("I"); 
					return traslatorRepository.save(traslator) 
							.then(Mono.just(ResponseEntity.status(HttpStatus.OK)
									.body("Eliminado exitoso!!"))); 
				})
				.switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("Traducción no encontrada"))) 
				.onErrorResume(error -> {
					log.error("Error marking translation as deleted: {}", error.getMessage());
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("Error al marcar la traducción como eliminado")); // Maneja cualquier error
				});
	}

}