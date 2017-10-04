package com.tmIndicadores.controller;

import com.tmIndicadores.controller.servicios.ProgramacionServicios;
import com.tmIndicadores.model.entity.Programacion;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("GoalProcessor")
public class IndicadoresGoalProcessor {

    @Autowired
    private ProcessorUtils processorUtils;

    @Autowired
    private ProgramacionServicios programacionServicios;

    private List<LogDatos> logDatos;
    private static Logger log = Logger.getLogger(IndicadoresGoalProcessor.class);
    private String destination = PathFiles.PATH;

    public List<LogDatos> processDataFromFile(String fileName, InputStream in, Date fechaProgramacion,String razon,String tipologia, String periocidad,String lineasC,String cuadro,String modo){
        logDatos = new ArrayList<>();
        logDatos.add(new LogDatos("<<Inicio Indicadores Goal Bus con Archivo>>", TipoLog.INFO));
        log.info("<<Inicio Indicadores Goal Bus con Archivo>>");
        if(!programacionServicios.isCuadroAlready(cuadro)){
            if(noExistenDatosParaLaFecha(fechaProgramacion,tipologia,periocidad)){
                processorUtils.copyFile(fileName,in,destination);
                destination=destination+fileName;
                readExcelAndSaveData(destination,fechaProgramacion,razon,tipologia,periocidad,lineasC,fileName,cuadro,modo);
            }else{
                logDatos.add(new LogDatos("Ya existe una programación para el dia "+periocidad+" "+fechaProgramacion.toString()+" Tipologia  "+tipologia, TipoLog.ERROR));
            }


        }else{
            logDatos.add(new LogDatos("El cuadro de programación "+cuadro+" ya existe", TipoLog.ERROR));
        }

        logDatos.add(new LogDatos("<<Fin Indicadores Goal Bus con Archivo>>", TipoLog.INFO));

        return logDatos;
    }

    private boolean noExistenDatosParaLaFecha(Date fechaProgramacion, String tipologia, String periocidad) {
        List<Programacion> programaciones = programacionServicios.getProgramacionbyFechaTipologiaPeriocidad(fechaProgramacion, tipologia, periocidad);
        if(programaciones.size()> 0) {
            return false;
        }
        return true;
    }

    private boolean elCuadroProgramacionNoExiste(String cuadro) {
        return false;
    }

    private void readExcelAndSaveData(String destination, Date fechaProgramacion, String razon,String tipologia, String periocidad,String lineasC,String filename,String cuadro,String modo) {
        BufferedReader br = null;
        String line = "";
        String previousLine ="";
        String cvsSplitBy = encontrarTipoArchivo(filename);
        int diffFiles = diferenciaIndexArchivos(cvsSplitBy);
        double km_vacio_inicial ;
        double km_comer_inicial ;

        try {

            br = new BufferedReader(new FileReader(destination));
            br.readLine(); // Leer encabezados
            line = br.readLine();
            String [] valores = line.split(cvsSplitBy);
            km_comer_inicial = Double.parseDouble(valores[TraceLogIndex.KM_LINEA-diffFiles]);
            km_vacio_inicial = Double.parseDouble(valores[TraceLogIndex.KM_VACIO-diffFiles]);
            while ((line = br.readLine()) != null){
                previousLine =line;
            }
            valores = previousLine.split(cvsSplitBy);
            Programacion programacion = new Programacion();
            programacion.setCuadro(cuadro);
            programacion.setFecha(fechaProgramacion);
            programacion.setRazonCambio(razon);
            programacion.setKmComercialIncio(km_comer_inicial);
            programacion.setKmVacioInicio(km_vacio_inicial);
            programacion.setPorcentajeVacioInicio(km_vacio_inicial/km_comer_inicial);
            programacion.setLineasCargadas(Integer.parseInt(lineasC));
            programacion.setKmComercialFin(Double.parseDouble(valores[TraceLogIndex.KM_LINEA-diffFiles]));
            programacion.setKmVacioFin(Double.parseDouble(valores[TraceLogIndex.KM_VACIO-diffFiles]));
            programacion.setPorcentajeVacioFinal(programacion.getKmVacioFin()/programacion.getKmComercialFin());
            programacion.setBuses(Integer.parseInt(valores[TraceLogIndex.NUM_BUSES-diffFiles]));
            programacion.setTiempoExpedicion(valores[TraceLogIndex.TOTAL_HORAS-diffFiles]);
            programacion.setExpedicionComercial(Integer.parseInt(valores[TraceLogIndex.NUM_VIAJES-diffFiles]));
            programacion.setTiempoProcesamiento(valores[TraceLogIndex.TIEMPO_SOLUCION-diffFiles]);
            programacion.setTipologia(tipologia);
            programacion.setPeriodicidad(periocidad);
            programacion.setTipoProgramacion("N");
            programacion.setModo(modo);

            programacionServicios.addProgramacion(programacion);

        } catch (FileNotFoundException e) {
           logDatos.add(new LogDatos(e.getMessage(), TipoLog.ERROR));
        } catch (IOException e) {
            logDatos.add(new LogDatos(e.getMessage(), TipoLog.ERROR));
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logDatos.add(new LogDatos(e.getMessage(), TipoLog.ERROR));
                }
            }
        }

    }

    private int diferenciaIndexArchivos(String cvsSplitBy) {
        if(cvsSplitBy.equals(";")){
            return 0;
        }
        return 1;
    }

    private String encontrarTipoArchivo(String filename) {

        String[] fileNameSplit = filename.split("\\.");
        if(fileNameSplit[1].equals("csv")){
            return ";";
        }
        return "\t";
    }

}
