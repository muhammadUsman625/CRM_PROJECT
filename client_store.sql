-- phpMyAdmin SQL Dump
-- version 4.7.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 06, 2018 at 04:17 PM
-- Server version: 10.1.24-MariaDB
-- PHP Version: 7.1.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `bits_crm`
--

-- --------------------------------------------------------

--
-- Table structure for table `client_store`
--

CREATE TABLE `client_store` (
  `CL_ID` int(11) NOT NULL,
  `CL_NAME` varchar(200) NOT NULL,
  `CL_CMPNY` varchar(200) NOT NULL,
  `CL_EMAIL` varchar(100) NOT NULL,
  `CL_PHONE` varchar(50) NOT NULL,
  `CL_ADDR` varchar(500) NOT NULL,
  `CL_CITY` varchar(100) NOT NULL,
  `CL_COUNTRY` varchar(100) NOT NULL,
  `CL_WEBSITE` varchar(200) NOT NULL,
  `CL_BCYCLE` date NOT NULL,
  `CL_TYPE` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `client_store`
--
ALTER TABLE `client_store`
  ADD PRIMARY KEY (`CL_ID`),
  ADD UNIQUE KEY `CL_ID` (`CL_ID`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
