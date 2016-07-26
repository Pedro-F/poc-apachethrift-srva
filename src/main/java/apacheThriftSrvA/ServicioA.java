package apacheThriftSrvA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@EnableAutoConfiguration
public class ServicioA {

	@RequestMapping(value = "/servicioA", method = RequestMethod.POST)
	public @ResponseBody PeticionSalida servicioAThrift(@RequestBody PeticionEntrada peticionEntrada,
											@RequestParam(value="param1", defaultValue="1") String size) {

		PeticionSalida responseMessage = new PeticionSalida();
		long iniTime = System.currentTimeMillis();
		long finTime = 0;
		long mediaTime = 0;
		int tamanio;
		
		try{
			tamanio = Integer.parseInt(size);
		}
		catch(Exception e){
			tamanio = 1;
		}
		
		try {

			// instance of input servicioB from input servicioA
			MensajeInServicioNoThrift inServicioB = new MensajeInServicioNoThrift(peticionEntrada);

			// invoke microservice B by HTTP-Rest
			RestTemplate restTemplate = new RestTemplate();
			MensajeOutServicioNoThrift outServicioB = null;
			
			iniTime = System.currentTimeMillis();
	        
			for(int i = 0;i<tamanio;i++){
				outServicioB = restTemplate.postForObject("http://no-thrift-srvb:8080/servicioB",
																				 inServicioB, 
																				 MensajeOutServicioNoThrift.class);
			}
			
			finTime = System.currentTimeMillis();

			// build servicioA output from servicioB response
			responseMessage = convertMensajeOutServicioNoThrift_TO_PeticionSalida(outServicioB);

		} catch (Exception e) {

			e.printStackTrace();

		}
		
		if ((finTime > iniTime) && (tamanio > 0)){
			mediaTime = (finTime - iniTime)/tamanio;
			System.out.println(".   FIN ServicioA [HTTP].  Se han lanzado " + tamanio + " Peticiones " +
			"En un tiempo de " + (finTime - iniTime) + " Milisegundos. La media es " + mediaTime + " Milisegundos");
		}
		else{
			System.out.println(".   FIN ServicioA. Numero de ejecuciones = " + tamanio +
					"Tiempo inicial = " + iniTime + ", Tiempo final = " + finTime + ".");
		}
		
		return responseMessage;
	}
	
	/**
	 * 
	 * @param outServicioB
	 * @return
	 */
	public PeticionSalida convertMensajeOutServicioNoThrift_TO_PeticionSalida(MensajeOutServicioNoThrift outServicioB){
		
		// prendas list instance 
		List<Prenda> prendas = new ArrayList<Prenda>();
		Prenda prenda = null;
		
		//PrendaNoThrift list instance to extract servicioB data
		List<PrendaNoThrift> prendasNoThrift = outServicioB.getCuerpo().get("Prendas");
		
		// If is not empty, we put prendaNoThrift on prendas list
		if (prendasNoThrift != null) {
			for (PrendaNoThrift prendaNoThrift : prendasNoThrift) {
				prenda = new Prenda();
				prenda.setColor(prendaNoThrift.getColor());
				prenda.setDescripcion(prendaNoThrift.getDescripcion());
				prenda.setNombre(prendaNoThrift.getNombre());
				prenda.setTalla(prendaNoThrift.getTalla());
				prenda.setTipo(Tipo.findByValue(prendaNoThrift.getTipo().getValue()));
				prenda.setStock(prendaNoThrift.getStock());
				prendas.add(prenda);
			}
		}

		// Create map to build response body of servicioA
		Map<String, List<Prenda>> cuerpoSalida = new HashMap<String, List<Prenda>>();
		cuerpoSalida.put("Prendas", prendas);
		
		// Build & Return PeticionSalida
		return new PeticionSalida(outServicioB.getCabecera(), cuerpoSalida, outServicioB.getAviso());
	}
	

	/*******************************************
	 * MAIN                                    *
	 *                                         *
	 * @param args                             *
	 *                                         *
	 * @throws Exception                       *
	 *                                         *
	 ******************************************/
	public static void main(String[] args) throws Exception {
		SpringApplication.run(ServicioA.class, args);
	}
}
