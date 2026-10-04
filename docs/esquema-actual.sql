/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19  Distrib 10.6.21-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: sgrea
-- ------------------------------------------------------
-- Server version	10.6.21-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bodega`
--

DROP TABLE IF EXISTS `bodega`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `bodega` (
  `id_bodega` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `descripcion` varchar(255) NOT NULL,
  PRIMARY KEY (`id_bodega`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conjunto`
--

DROP TABLE IF EXISTS `conjunto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `conjunto` (
  `id_conjunto` varchar(50) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_estado_conjunto` int(11) NOT NULL,
  `fecha_alta` datetime NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_conjunto`),
  KEY `idx_conjunto_producto` (`id_producto`),
  KEY `idx_conjunto_estado` (`id_estado_conjunto`),
  CONSTRAINT `fk_conjunto_estado` FOREIGN KEY (`id_estado_conjunto`) REFERENCES `estadoconjunto` (`id_estado_conjunto`),
  CONSTRAINT `fk_conjunto_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contenedor`
--

DROP TABLE IF EXISTS `contenedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `contenedor` (
  `id_contenedor` int(11) NOT NULL AUTO_INCREMENT,
  `codigo` varchar(50) NOT NULL,
  `capacidad` int(11) NOT NULL,
  `id_estado_contenedor` int(11) NOT NULL,
  `fecha_alta` datetime NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_contenedor`),
  UNIQUE KEY `uk_contenedor_codigo` (`codigo`),
  KEY `idx_contenedor_estado` (`id_estado_contenedor`),
  CONSTRAINT `fk_contenedor_estado` FOREIGN KEY (`id_estado_contenedor`) REFERENCES `estadocontenedor` (`id_estado_contenedor`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `detallehoja`
--

DROP TABLE IF EXISTS `detallehoja`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `detallehoja` (
  `id_detalle` int(11) NOT NULL AUTO_INCREMENT,
  `id_hoja` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `cantidad_solicitada` int(11) NOT NULL,
  `cantidad_surtida` int(11) DEFAULT NULL,
  `cantidad_devuelta` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id_detalle`),
  UNIQUE KEY `uk_detalle_hoja_producto` (`id_hoja`,`id_producto`),
  KEY `fk_detalle_producto` (`id_producto`),
  CONSTRAINT `fk_detalle_hoja` FOREIGN KEY (`id_hoja`) REFERENCES `hojaproduccion` (`id_hoja`),
  CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`id_producto`) REFERENCES `producto` (`id_producto`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `estadoconjunto`
--

DROP TABLE IF EXISTS `estadoconjunto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadoconjunto` (
  `id_estado_conjunto` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id_estado_conjunto`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `estadocontenedor`
--

DROP TABLE IF EXISTS `estadocontenedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadocontenedor` (
  `id_estado_contenedor` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id_estado_contenedor`),
  UNIQUE KEY `uk_estado_contenedor_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `estadohoja`
--

DROP TABLE IF EXISTS `estadohoja`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadohoja` (
  `id_estado_hoja` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_estado_hoja`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hojacontenedor`
--

DROP TABLE IF EXISTS `hojacontenedor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hojacontenedor` (
  `id_hoja_contenedor` int(11) NOT NULL AUTO_INCREMENT,
  `id_hoja` int(11) NOT NULL,
  `id_contenedor` int(11) NOT NULL,
  `fecha_asignacion` datetime NOT NULL,
  `fecha_cierre_carga` datetime DEFAULT NULL,
  `fecha_liberacion` datetime DEFAULT NULL,
  `id_usuario_asignacion` int(11) NOT NULL,
  `id_usuario_cierre` int(11) DEFAULT NULL,
  `id_usuario_liberacion` int(11) DEFAULT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_hoja_contenedor`),
  KEY `fk_hojacontenedor_usuario_asignacion` (`id_usuario_asignacion`),
  KEY `fk_hojacontenedor_usuario_cierre` (`id_usuario_cierre`),
  KEY `fk_hojacontenedor_usuario_liberacion` (`id_usuario_liberacion`),
  KEY `idx_hojacontenedor_hoja` (`id_hoja`),
  KEY `idx_hojacontenedor_contenedor_liberacion` (`id_contenedor`,`fecha_liberacion`),
  CONSTRAINT `fk_hojacontenedor_contenedor` FOREIGN KEY (`id_contenedor`) REFERENCES `contenedor` (`id_contenedor`),
  CONSTRAINT `fk_hojacontenedor_hoja` FOREIGN KEY (`id_hoja`) REFERENCES `hojaproduccion` (`id_hoja`),
  CONSTRAINT `fk_hojacontenedor_usuario_asignacion` FOREIGN KEY (`id_usuario_asignacion`) REFERENCES `usuario` (`id_usuario`),
  CONSTRAINT `fk_hojacontenedor_usuario_cierre` FOREIGN KEY (`id_usuario_cierre`) REFERENCES `usuario` (`id_usuario`),
  CONSTRAINT `fk_hojacontenedor_usuario_liberacion` FOREIGN KEY (`id_usuario_liberacion`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hojaproduccion`
--

DROP TABLE IF EXISTS `hojaproduccion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hojaproduccion` (
  `id_hoja` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_proyecto` varchar(100) NOT NULL,
  `cliente` varchar(100) NOT NULL,
  `fecha_salida` datetime NOT NULL,
  `fecha_estimada_regreso` datetime DEFAULT NULL,
  `id_bodega` int(11) NOT NULL,
  `id_estado_hoja` int(11) NOT NULL,
  PRIMARY KEY (`id_hoja`),
  KEY `fk_hoja_bodega` (`id_bodega`),
  KEY `fk_hoja_estado` (`id_estado_hoja`),
  CONSTRAINT `fk_hoja_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodega` (`id_bodega`),
  CONSTRAINT `fk_hoja_estado` FOREIGN KEY (`id_estado_hoja`) REFERENCES `estadohoja` (`id_estado_hoja`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `movimiento`
--

DROP TABLE IF EXISTS `movimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `movimiento` (
  `id_movimiento` int(11) NOT NULL AUTO_INCREMENT,
  `id_tipo_movimiento` int(11) NOT NULL,
  `id_conjunto` varchar(50) NOT NULL,
  `id_hoja` int(11) NOT NULL,
  `id_detalle` int(11) DEFAULT NULL,
  `id_hoja_contenedor` int(11) DEFAULT NULL,
  `fecha` datetime NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_movimiento`),
  KEY `fk_movimiento_tipo` (`id_tipo_movimiento`),
  KEY `fk_movimiento_detalle` (`id_detalle`),
  KEY `idx_movimiento_hoja_fecha` (`id_hoja`,`fecha`),
  KEY `idx_movimiento_conjunto_fecha` (`id_conjunto`,`fecha`),
  KEY `idx_movimiento_usuario_fecha` (`id_usuario`,`fecha`),
  KEY `idx_movimiento_hojacontenedor_fecha` (`id_hoja_contenedor`,`fecha`),
  CONSTRAINT `fk_movimiento_conjunto` FOREIGN KEY (`id_conjunto`) REFERENCES `conjunto` (`id_conjunto`),
  CONSTRAINT `fk_movimiento_detalle` FOREIGN KEY (`id_detalle`) REFERENCES `detallehoja` (`id_detalle`),
  CONSTRAINT `fk_movimiento_hoja` FOREIGN KEY (`id_hoja`) REFERENCES `hojaproduccion` (`id_hoja`),
  CONSTRAINT `fk_movimiento_hojacontenedor` FOREIGN KEY (`id_hoja_contenedor`) REFERENCES `hojacontenedor` (`id_hoja_contenedor`),
  CONSTRAINT `fk_movimiento_tipo` FOREIGN KEY (`id_tipo_movimiento`) REFERENCES `tipomovimiento` (`id_tipo_movimiento`),
  CONSTRAINT `fk_movimiento_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `producto`
--

DROP TABLE IF EXISTS `producto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `producto` (
  `id_producto` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `descripcion` varchar(255) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `activo` tinyint(1) NOT NULL,
  `url_imagen` varchar(255) NOT NULL,
  PRIMARY KEY (`id_producto`),
  KEY `fk_producto_bodega` (`id_bodega`),
  CONSTRAINT `fk_producto_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodega` (`id_bodega`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `id_rol` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_rol`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sesion`
--

DROP TABLE IF EXISTS `sesion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesion` (
  `id_sesion` int(11) NOT NULL AUTO_INCREMENT,
  `id_bodega` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `token` varchar(255) NOT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime DEFAULT NULL,
  `activa` tinyint(1) NOT NULL,
  PRIMARY KEY (`id_sesion`),
  UNIQUE KEY `token` (`token`),
  KEY `fk_sesion_usuario` (`id_usuario`),
  KEY `fk_sesion_bodega` (`id_bodega`),
  CONSTRAINT `fk_sesion_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodega` (`id_bodega`),
  CONSTRAINT `fk_sesion_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tipomovimiento`
--

DROP TABLE IF EXISTS `tipomovimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipomovimiento` (
  `id_tipo_movimiento` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  PRIMARY KEY (`id_tipo_movimiento`),
  UNIQUE KEY `uk_tipomovimiento_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id_usuario` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `activo` tinyint(1) NOT NULL,
  `id_rol` int(11) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `username` (`username`),
  KEY `fk_usuario_rol` (`id_rol`),
  CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usuariobodega`
--

DROP TABLE IF EXISTS `usuariobodega`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuariobodega` (
  `id_bodega` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  PRIMARY KEY (`id_bodega`,`id_usuario`),
  KEY `fk_usuariobodega_usuario` (`id_usuario`),
  CONSTRAINT `fk_usuariobodega_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodega` (`id_bodega`),
  CONSTRAINT `fk_usuariobodega_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping events for database 'sgrea'
--

--
-- Dumping routines for database 'sgrea'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-27 18:40:29
