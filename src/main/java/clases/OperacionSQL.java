package clases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que realiza las operaciones básicas de SQL para la gestión de usuarios
 * e imagenes
 *
 * @author alumne
 */
public class OperacionSQL {

    private Connection conn;
    private PreparedStatement statement;

    public OperacionSQL() {

    }

    private void abrirConexion() throws Exception { // Throws porque excepciones siempre serán controladas (método privado)
        Class.forName("org.apache.derby.jdbc.ClientDriver");
        conn = DriverManager.getConnection("jdbc:derby://localhost:1527/pr2;user=pr2;password=pr2");
    }

    private void cerrarConexion() { //Aquí al ejecutarse en el finally, se controla la excepción aparte
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            // connection close failed.
            System.err.println(e.getMessage());
        }
    }

   

////////////OPERACIONES RELACIONADAS CON USUARIO////////////
    public boolean validarUsuario(String usuario, String password) {
        boolean resultado = false;
        try {
            abrirConexion();
            String query = "SELECT * FROM usuarios WHERE id_usuario = ? AND password = ?";
            statement = conn.prepareStatement(query);
            statement.setString(1, usuario);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            resultado = rs.next();

        } catch (Exception ex) {
            System.err.println(ex.getMessage());

        } finally {
            cerrarConexion();
        }
        return resultado;
    }

////////////OPERACIONES RELACIONADAS CON IMAGEN////////////
    
    //Instanciar Imagen (dado un resultado de búsqueda de imagen, devuelve un objeto imagen con todos sus atributos)
    private Imagen instanciarImagen(ResultSet rs) throws SQLException { // Throws porque excepciones siempre serán controladas (método privado)
        return new Imagen(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("keywords"),
                rs.getString("author"),
                rs.getString("creator"),
                rs.getString("capture_date"),
                rs.getString("storage_date"),
                rs.getString("filename")
        );
    }
    
    public boolean insertarImagen(Imagen img) {
        boolean resultado = false;
        try {
            abrirConexion();
            String sql = "INSERT INTO image (title, description, keywords, author, creator, capture_date, storage_date, filename) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            statement = conn.prepareStatement(sql);
            statement.setString(1, img.getTitulo());
            statement.setString(2, img.getDescripcion());
            statement.setString(3, img.getKeywords());
            statement.setString(4, img.getAutor());
            statement.setString(5, img.getCreador());
            statement.setString(6, img.getFechaCreacion());
            statement.setString(7, img.getFechaAlta());
            statement.setString(8, img.getNombreFichero());
            resultado = statement.executeUpdate() > 0;

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            cerrarConexion();
        }
        return resultado;
    }

    public List<Imagen> buscarImagenesPorCreador(String creador) {
        List<Imagen> lista = new ArrayList<>();
        try {
            abrirConexion();
            String query = "SELECT id, title, description, keywords, author, creator, capture_date, storage_date, filename "
                    + "FROM image WHERE creator = ? ORDER BY storage_date DESC";

            statement = conn.prepareStatement(query);
            statement.setString(1, creador);

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                lista.add(instanciarImagen(rs));
            }

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            cerrarConexion();
        }
        return lista;
    }

    public List<Imagen> buscarImagenes(String titulo,String keywords,String autor,String fechaCreacion,String creador) {
        List<Imagen> lista = new ArrayList<>(); //DUDA: RUBRICA: ES EL UNICO SITIO DONDE ESTOY USANDO LIKE PQ EN LOS DEMÁS CREO QUE NO ES CORRECTO, ESTA BIEN???
        try {
            abrirConexion();
            //Vamos añadiendo parámetros a la búsqueda en función de si son null o no
            StringBuilder query = new StringBuilder();
            query.append("SELECT id, title, description, keywords, author, creator, capture_date, storage_date, filename");
            query.append(" FROM image WHERE 1=1");

            if (titulo != null) {
                query.append(" AND title LIKE ?");
            }
            if (keywords != null) {
                query.append(" AND keywords LIKE ?");
            }
            if (autor != null) {
                query.append(" AND author LIKE ?");
            }
            if (fechaCreacion != null) {
                query.append(" AND capture_date = ?");
            }

            query.append(" ORDER BY storage_date DESC");
            statement = conn.prepareStatement(query.toString());
            
            //Adición de parámetros a la consulta (Usamos LIKE y %, por si el usuario no ha metido exactamente el parámetro)
            int indice = 1;
            //statement.setString(indice++, creador);
            if (titulo != null) {
                statement.setString(indice++, "%" + titulo + "%");
            }
            if (keywords != null) {
                statement.setString(indice++, "%" + keywords + "%");
            }
            if (autor != null) {
                statement.setString(indice++, "%" + autor + "%");
            }
            if (fechaCreacion != null) {
                statement.setString(indice++, fechaCreacion);
            }

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                lista.add(instanciarImagen(rs));
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            cerrarConexion();
        }
        return lista;
    }
    
    public Imagen obtenerImagenPorId(int id) {
        Imagen img = null;
        try {
            abrirConexion();
            String query = "SELECT id, title, description, keywords, author, creator, capture_date, storage_date, filename " +
                         "FROM image WHERE id = ?";
            statement = conn.prepareStatement(query);
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                img = instanciarImagen(rs);
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            cerrarConexion();
        }
        return img;
    }

    public boolean actualizarImagen(Imagen img) {
        boolean resultado = false;
        try {
            abrirConexion();
            String sql = "UPDATE image SET title=?, description=?, keywords=?, author=?, capture_date=? " +
                         "WHERE id = ? AND creator = ?";
            statement = conn.prepareStatement(sql);
            statement.setString(1, img.getTitulo());
            statement.setString(2, img.getDescripcion());
            statement.setString(3, img.getKeywords());
            statement.setString(4, img.getAutor());
            statement.setString(5, img.getFechaCreacion());
            statement.setInt(6, img.getId());
            statement.setString(7, img.getCreador());
            resultado = statement.executeUpdate() > 0;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        } finally {
            cerrarConexion();
        }
        return resultado;
    }
    
    public boolean eliminarImagen(int id){
        boolean resultado = false;
        try{
            abrirConexion();
            String sql = "DELETE FROM image WHERE id = ?";
            statement = conn.prepareStatement(sql);
            statement.setInt(1, id);
            resultado = statement.executeUpdate() > 0;
        }catch (Exception ex){
            System.err.println(ex.getMessage());
        }finally{
            cerrarConexion();
        }
        return resultado;
    }
}
