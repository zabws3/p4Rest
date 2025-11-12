package test.p4rest.resources;


import clases.OperacionSQL;
import clases.Imagen;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author
 */
@Path("jakartaee9")
public class JakartaEE91Resource {

    private String imagenAJson(Imagen img) {
        if (img == null) {
            return "{}";
        }

        return String.format(
                "{"
                + "\"id\":%d,"
                + "\"title\":\"%s\","
                + "\"description\":\"%s\","
                + "\"keywords\":\"%s\","
                + "\"author\":\"%s\","
                + "\"creator\":\"%s\","
                + "\"capture_date\":\"%s\","
                + "\"storage_date\":\"%s\","
                + "\"filename\":\"%s\""
                + "}",
                img.getId(),
                img.getTitulo(),
                img.getDescripcion(),
                img.getKeywords(),
                img.getAutor(),
                img.getCreador(),
                img.getFechaCreacion(),
                img.getFechaAlta(),
                img.getNombreFichero() //DUDA: Hay q controlar q el usuario ponga en los atributos /, %, ", ...
        );
    }

    private String listaImagenesAJson(List<Imagen> imagenes) {

        if (imagenes == null || imagenes.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < imagenes.size(); i++) {
            json.append(imagenAJson(imagenes.get(i)));

            if (i < imagenes.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");

        return json.toString();
    }

    @GET
    public Response ping() {
        return Response
                .ok("ping Jakarta EE")
                .build();
    }

    /**
     * POST method to login in the application
     *
     * @param username
     * @param password
     * @return
     */
    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username, @FormParam("password") String password) {

        OperacionSQL op = new OperacionSQL();
        boolean loginVerificado = op.validarUsuario(username, password);
        if (loginVerificado) {
            //Login correcto
            return Response.ok().build();
        } else {
            //Login incorrecto
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    /**
     * POST method to register a new image – File is not uploaded
     *
     * @param title
     * @param description
     * @param keywords
     * @param author
     * @param creator
     * @param capt_date
     * @return
     */
    
    
    @Path("register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerImage(@FormParam("title") String title,
            @FormParam("description") String description,
            @FormParam("keywords") String keywords,
            @FormParam("author") String author,
            @FormParam("creator") String creator,
            @FormParam("capture") String capt_date //DUDA: LOS ATRIBUTOS FECHA ALTA Y NOMBRE FICHERO???
    ) {

        if (title == null || title.isEmpty() || creator == null || creator.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaAlta = sdf.format(new Date());
        Imagen img = new Imagen(title, description, keywords, author, creator, capt_date, fechaAlta, "");
        OperacionSQL op = new OperacionSQL();
        // Insertar la imagen en la base de datos
        boolean registroExitoso = op.insertarImagen(img);

        if (registroExitoso) {
            // Registro exitoso, devolvemos solo estado 200
            return Response.ok().build();
        } else {
            // Error en el registro, devolvemos error interno
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }


    
    
    
    
    /**
     * POST method to modify an existing image
     *
     * @param id
     * @param title
     * @param description
     * @param keywords
     * @param author
     * @param creator, used for checking image ownership
     * @param capt_date
     * @return
     */
    @Path("modify")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyImage(
            @FormParam("id") String id,
            @FormParam("title") String title,
            @FormParam("description") String description,
            @FormParam("keywords") String keywords,
            @FormParam("author") String author,
            @FormParam("creator") String creator,
            @FormParam("capture") String capt_date) {

        if (id == null || id.isEmpty() || creator == null || creator.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        //Obtener la imagen actual para validar que el usuario es el creador
        OperacionSQL op = new OperacionSQL();
        Imagen imgActual = op.obtenerImagenPorId(Integer.parseInt(id));

        //Verificar que la imagen existe
        if (imgActual == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        //Verificar que el usuario es el creador (propiedad)
        if (!imgActual.getCreador().equals(creator)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        //Crear objeto Imagen con los nuevos datos
        //fechaAta y filename se mantienen (temporalmente)
        Imagen imgNueva = new Imagen(
                Integer.parseInt(id),
                title,
                description,
                keywords,
                author,
                creator,
                capt_date,
                imgActual.getFechaAlta(),
                imgActual.getNombreFichero()
        );

        boolean modificacionExitosa = op.actualizarImagen(imgNueva);

        if (modificacionExitosa) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST method to delete an existing image
     *
     * @param id
     * @param creator
     * @return
     */
    @Path("delete")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteImage(@FormParam("id") String id,
            @FormParam("creator") String creator) {
        OperacionSQL op = new OperacionSQL();
        Imagen imgActual = op.obtenerImagenPorId(Integer.parseInt(id));

        //Verificar que la imagen existe
        if (imgActual == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        //Verificar que el usuario es el creador
        if (!imgActual.getCreador().equals(creator)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        boolean eliminacionExitosa = op.eliminarImagen(Integer.parseInt(id));

        if (eliminacionExitosa) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * GET method to search images by id
     *
     * @param id
     * @return
     */
    @Path("searchID/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByID(@PathParam("id") int id) {
        OperacionSQL op = new OperacionSQL();
        Imagen img = op.obtenerImagenPorId(id);

        if (img == null) {
            // Imagen no encontrada
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(imagenAJson(img)).build();

    }

    /**
     * GET method to search images by title
     *
     * @param title
     * @return
     */
    @Path("searchTitle/{title}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByTitle(@PathParam("title") String title) { //DUDA: no me tendrían que pasar tmb el nombre de usuario? NO, CONTROLA CLIENTE!

        OperacionSQL op = new OperacionSQL();
        List<Imagen> imagenes = op.buscarImagenes(title, null, null, null, null);
        if (imagenes == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(listaImagenesAJson(imagenes)).build();

    }

    /**
     * GET method to search images by creation date. Date format should be
     * yyyy-mm-dd
     *
     * @param date
     * @return
     */
    @Path("searchCreationDate/{date}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByCreationDate(@PathParam("date") String date) {

        OperacionSQL op = new OperacionSQL();
        List<Imagen> imagenes = op.buscarImagenes(null, null, null, date, null);
        if (imagenes == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(listaImagenesAJson(imagenes)).build();

    }

    /**
     * GET method to search images by title Servicio extra!
     *
     * @param creator
     * @return
     */
    @Path("searchCreator/{creator}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByCreator(@PathParam("creator") String creator) {

        OperacionSQL op = new OperacionSQL();
        List<Imagen> imagenes = op.buscarImagenesPorCreador(creator); //MODIFICAR MÉTODO EN CASO DE QUE NO HAGA FALTA EL CREADOR!!!!
        if (imagenes == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(listaImagenesAJson(imagenes)).build();
    }

    /**
     * GET method to search images by title Servicio extra!
     *
     * @param author
     * @return
     */
    @Path("searchAuthor/{author}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByAuthor(@PathParam("author") String author) {

        OperacionSQL op = new OperacionSQL();
        List<Imagen> imagenes = op.buscarImagenes(null, null, author, null, null);
        if (imagenes == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(listaImagenesAJson(imagenes)).build();

    }
    
    @Path("searchKeywords/{keywords}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchByKeywords(@PathParam("keywords") String keywords) {

        OperacionSQL op = new OperacionSQL();
        List<Imagen> imagenes = op.buscarImagenes(null, keywords, null, null, null);
        if (imagenes == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(listaImagenesAJson(imagenes)).build();

    }
}
