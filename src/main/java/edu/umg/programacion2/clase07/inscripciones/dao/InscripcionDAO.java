package edu.umg.programacion2.clase07.inscripciones.dao;

import edu.umg.programacion2.clase07.inscripciones.modelo.Curso;
import edu.umg.programacion2.clase07.inscripciones.modelo.Estudiante;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TAREA: este es el DAO que tienes que construir. Resuelve la relacion
 * muchos-a-muchos entre estudiantes y cursos (tabla intermedia
 * `inscripciones`).
 *
 * Cada metodo trae: que debe hacer, un ejemplo de entrada/salida con los
 * datos de sql/schema.sql, y pistas (no la solucion completa). Revisa
 * EstudianteDAO/CursoDAO de este mismo proyecto y PrestamoDAO de
 * clase07-biblioteca-jdbc como referencia de estilo - el patron
 * (PreparedStatement + try-with-resources) es siempre el mismo, lo que
 * cambia es la consulta SQL.
 */
public class InscripcionDAO {

    private static final String URL = "jdbc:mysql://localhost:3306/prog2_db?useSSL=false&serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "Darlery2006.";

    /**
     * Inscribe a un estudiante en un curso. Retorna el id generado.
     *
     * Ejemplo: inscribir(4, 3) inscribe a Luis Ramirez (id 4) en Matematica
     * Discreta (id 3, todavia no tiene ninguna inscripcion en los datos de
     * ejemplo).
     *
     * Pistas:
     * 1. INSERT INTO inscripciones (estudiante_id, curso_id) VALUES (?, ?)
     *    (nota se deja NULL: todavia no la tiene).
     * 2. Recuerda Statement.RETURN_GENERATED_KEYS + getGeneratedKeys(), como
     *    en EstudianteDAO.crear().
     * 3. IMPORTANTE - caso nuevo que no viste en la Clase 5: la tabla tiene
     *    UNIQUE (estudiante_id, curso_id). Si alguien ya esta inscrito en
     *    ese curso, MySQL lanza una excepcion especifica:
     *    SQLIntegrityConstraintViolationException (es una SUBCLASE de
     *    SQLException, por eso puedes atraparla en un catch por separado,
     *    ANTES del catch de SQLException general). Atrapala y retorna -1 en
     *    vez de dejar que el error se propague sin explicacion.
     */
    public int inscribir(int estudianteId, int cursoId) throws SQLException {
        String sql= "INSERT INTO inscripciones (estudiante_id, curso_id) VALUES (?, ?)";
        
        try (Connection conexion = DriverManager.getConnection(URL,USUARIO, PASSWORD);
        PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
        	
        	statement.setInt(1, estudianteId);
        	statement.setInt(2, cursoId);
        	statement.executeUpdate();
        	
        try (ResultSet claves = statement.getGeneratedKeys()){
        	if(claves.next()) {
        		return claves.getInt(1);
        	}
        	return -1;
        }
        
        } catch (SQLIntegrityConstraintViolationException e) {
        return -1;
    }
    }

    /**
     * Registra (o actualiza) la nota de un estudiante en un curso.
     *
     * Ejemplo: registrarNota(3, 3, 88.0) le pone 88.0 a la inscripcion de
     * Maria Gonzalez (id 3) en Matematica Discreta (id 3), que en los datos
     * de ejemplo esta con nota NULL.
     *
     * Pistas:
     * 1. UPDATE inscripciones SET nota = ? WHERE estudiante_id = ? AND curso_id = ?
     * 2. Retorna true si executeUpdate() afecto al menos una fila, false si
     *    esa pareja estudiante/curso no existe (mismo patron de
     *    EstudianteDAO.actualizarNombre en la Clase 5).
     */
    public boolean registrarNota(int estudianteId, int cursoId, double nota) throws SQLException {
        String sql = "UPDATE inscripciones SET nota = ? WHERE estudiante_id = ? AND curso_id = ?";
        
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
        		PreparedStatement statement = conexion.prepareStatement(sql)){
        	
        	statement.setDouble(1, nota);
        	statement.setInt(2, estudianteId);
        	statement.setInt(3, cursoId);
        	
        	int filasAfectadas = statement.executeUpdate();
        	return filasAfectadas > 0;
        }
    }

    /**
     * Lista los cursos en los que esta inscrito un estudiante, dado su
     * carnet.
     *
     * Ejemplo: listarCursosDeEstudiante("2024001") (Ana Lopez) devuelve
     * Programacion 2 y Base de Datos 1 (en los datos de ejemplo).
     *
     * Pistas:
     * 1. Necesitas un JOIN de TRES tablas:
     *      SELECT c.id, c.nombre, c.creditos
     *      FROM inscripciones i
     *      JOIN cursos c ON i.curso_id = c.id
     *      JOIN estudiantes e ON i.estudiante_id = e.id
     *      WHERE e.carnet = ?
     * 2. Mira PrestamoDAO.listarPrestamosActivosConLibro() en
     *    clase07-biblioteca-jdbc si necesitas repasar como se mapea un
     *    ResultSet que viene de un JOIN.
     */
    public List<Curso> listarCursosDeEstudiante(String carnet) throws SQLException {
    	String sql = "SELECT c.id, c.nombre, c.creditos "
    			   + "FROM inscripciones i "
    			   + "Join cursos c ON i.curso_id = c.id "
    			   + "Join estudiantes e ON i.estudiante_id = e.id "
    			   + "Where e.carnet = ?";
    	
        List<Curso> resultado = new ArrayList<>();
        
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD );
        	 PreparedStatement statement = conexion.prepareStatement(sql)){
        	
        	statement.setString(1, carnet);
        	
        	try (ResultSet rs = statement.executeQuery()){
        		while (rs.next()) {
        			int id = rs.getInt("id");
        			String nombre = rs.getString("nombre");
        			int creditos = rs.getInt("creditos");
        			resultado.add(new Curso (id, nombre, creditos));
        		}
        	}
        }	
    
        return resultado;
    }

    /**
     * Lista los estudiantes inscritos en un curso, dado su nombre.
     *
     * Ejemplo: listarEstudiantesDeCurso("Programacion 2") devuelve Ana
     * Lopez, Carlos Perez y Maria Gonzalez (en los datos de ejemplo).
     *
     * Pistas: es el JOIN "espejo" del metodo anterior - misma idea, pero
     * seleccionando columnas de `estudiantes` y filtrando por `c.nombre`.
     */
    public List<Estudiante> listarEstudiantesDeCurso(String nombreCurso) throws SQLException {
    	String sql = "SELECT e.id, e.nombre, e.carnet "
    			   + "FROM inscripciones i "
    			   + "JOIN estudiantes e ON i.estudiante_id = e.id "
    			   + "JOIN cursos c ON i.curso_id = c.id "
    			   + "WHERE c.nombre = ?";
    	
        List<Estudiante> resultado = new ArrayList<>();
        
        try (Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
                PreparedStatement statement = conexion.prepareStatement(sql)) {

               statement.setString(1, nombreCurso);

               try (ResultSet rs = statement.executeQuery()) {
                   while (rs.next()) {
                       int id = rs.getInt("id");
                       String nombre = rs.getString("nombre");
                       String carnet = rs.getString("carnet");
                       resultado.add(new Estudiante(id, nombre, carnet));
                   }
               }
           }

        return resultado;
    }

    /**
     * Calcula el promedio de notas de un estudiante (solo cursos que YA
     * tienen nota registrada).
     *
     * Ejemplo: promedioDeEstudiante("2024001") (Ana Lopez, notas 90.00 y
     * 85.00) devuelve Optional.of(87.5).
     *
     * Pistas:
     * 1. Esto es un caso nuevo: en vez de traer las filas y promediar en
     *    Java con una lista, se lo pides a MySQL con una funcion de
     *    AGREGACION:
     *      SELECT AVG(i.nota) AS promedio
     *      FROM inscripciones i
     *      JOIN estudiantes e ON i.estudiante_id = e.id
     *      WHERE e.carnet = ?
     * 2. IMPORTANTE: AVG() ignora automaticamente las filas con nota NULL -
     *    no necesitas filtrarlas a mano.
     * 3. Si el estudiante no tiene NINGUNA nota registrada, AVG devuelve
     *    NULL. Revisa eso con resultado.getObject("promedio") == null (o
     *    resultado.wasNull() despues de getDouble) antes de retornar
     *    Optional.empty().
     */
    public Optional<Double> promedioDeEstudiante(String carnet) throws SQLException {
    String sql = "SELECT AVG(i.nota) As promedio "
    		   + "FROM inscripciones i "
    		   + "JOIN estudiantes e ON i.estudiante_id = e.id "
    		   + "WHERE e.carnet = ?";
    
    try (Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
            PreparedStatement statement = conexion.prepareStatement(sql)) {

           statement.setString(1, carnet);

           try (ResultSet rs = statement.executeQuery()) {
               if (rs.next()) {
                   double promedio = rs.getDouble("promedio");
                   if (!rs.wasNull()) {
                       return Optional.of(promedio);
                   }
               }
           }
       }
        return Optional.empty();
    }

    /**
     * Encuentra el nombre del curso con mas estudiantes inscritos.
     *
     * Ejemplo: con los datos de sql/schema.sql, "Programacion 2" tiene 3
     * inscritos (Ana, Carlos, Maria) y es el que mas tiene.
     *
     * Pistas:
     * 1. Otra consulta de agregacion, esta vez con GROUP BY:
     *      SELECT c.nombre, COUNT(*) AS total
     *      FROM inscripciones i
     *      JOIN cursos c ON i.curso_id = c.id
     *      GROUP BY c.nombre
     *      ORDER BY total DESC
     *      LIMIT 1
     * 2. GROUP BY agrupa las filas por curso antes de contar; sin GROUP BY,
     *    COUNT(*) contaria TODAS las inscripciones juntas, sin separar por
     *    curso.
     * 3. Con LIMIT 1 le pides a MySQL que ya te de solo el primero (el mas
     *    inscrito); no necesitas traer todos y comparar en Java.
     * 4. Si no hay ninguna inscripcion todavia, el ResultSet viene vacio:
     *    retorna Optional.empty() en ese caso.
     */
    public Optional<String> cursoConMasInscritos() throws SQLException {
    	String sql = "SELECT c.nombre, COUNT(*) AS total "
                + "FROM inscripciones i "
                + "JOIN cursos c ON i.curso_id = c.id "
                + "GROUP BY c.nombre "
                + "ORDER BY total DESC "
                + "LIMIT 1";

     try (Connection conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
          PreparedStatement statement = conexion.prepareStatement(sql);
          ResultSet rs = statement.executeQuery()) {

         if (rs.next()) {
             return Optional.of(rs.getString("nombre"));
         }
     } 
        return Optional.empty();
    }
}
