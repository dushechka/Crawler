-- --------------------------------------------------------
-- Хост:                         127.0.0.1
-- Версия сервера:               10.3.7-MariaDB - mariadb.org binary distribution
-- Операционная система:         Win64
-- HeidiSQL Версия:              9.4.0.5125
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Дамп структуры базы данных searchandratewords
DROP DATABASE IF EXISTS `searchandratewords`;
CREATE DATABASE IF NOT EXISTS `searchandratewords` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `searchandratewords`;

-- Дамп структуры для таблица searchandratewords.keywords
CREATE TABLE IF NOT EXISTS `keywords` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `personID` int(11) NOT NULL,
  `name` varchar(2048) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_keywords_persons` (`personID`),
  CONSTRAINT `FK_keywords_persons` FOREIGN KEY (`personID`) REFERENCES `persons` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.keywords: ~4 rows (приблизительно)
/*!40000 ALTER TABLE `keywords` DISABLE KEYS */;
INSERT INTO `keywords` (`ID`, `personID`, `name`) VALUES
	(1, 1, 'Путина'),
	(2, 1, 'Путину'),
	(3, 1, 'Путиным'),
	(4, 1, 'Путине');
/*!40000 ALTER TABLE `keywords` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.log
CREATE TABLE IF NOT EXISTS `log` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `adminID` int(11) NOT NULL,
  `action` varchar(2048) NOT NULL,
  `logDate` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`ID`),
  KEY `FK_log_users` (`adminID`),
  CONSTRAINT `FK_log_users` FOREIGN KEY (`adminID`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.log: ~9 rows (приблизительно)
/*!40000 ALTER TABLE `log` DISABLE KEYS */;
INSERT INTO `log` (`ID`, `adminID`, `action`, `logDate`) VALUES
	(1, 1, 'Добавил Admin2', '2018-05-28 18:55:02'),
	(2, 2, 'Добавил User1', '2018-05-28 18:55:30'),
	(3, 1, 'Добавил User2', '2018-05-28 18:57:53'),
	(4, 1, 'Добавил сайт lenta.ru', '2018-05-28 19:00:45'),
	(5, 1, 'Добавил персону Путин', '2018-05-28 19:01:27'),
	(6, 1, 'Добавил Путина', '2018-05-28 19:35:14'),
	(7, 1, 'Добавил Путину', '2018-05-28 19:36:39'),
	(8, 1, 'Добавил Путиным', '2018-05-28 19:37:37'),
	(9, 1, 'Добавил Путине', '2018-05-28 19:38:00');
/*!40000 ALTER TABLE `log` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.pages
CREATE TABLE IF NOT EXISTS `pages` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `URL` varchar(1024) NOT NULL,
  `siteID` int(11) NOT NULL,
  `foundDateTime` datetime DEFAULT NULL,
  `lastScanDate` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `URL` (`URL`),
  KEY `FK_pages_sites` (`siteID`),
  CONSTRAINT `FK_pages_sites` FOREIGN KEY (`siteID`) REFERENCES `sites` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.pages: ~1 rows (приблизительно)
/*!40000 ALTER TABLE `pages` DISABLE KEYS */;
INSERT INTO `pages` (`ID`, `URL`, `siteID`, `foundDateTime`, `lastScanDate`) VALUES
	(1, 'https://lenta.ru/news/2018/05/24/putin_vs_trump/', 1, '2018-05-28 19:43:14', NULL);
/*!40000 ALTER TABLE `pages` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.persons
CREATE TABLE IF NOT EXISTS `persons` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(2048) NOT NULL,
  `addedBy` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_persons_users` (`addedBy`),
  CONSTRAINT `FK_persons_users` FOREIGN KEY (`addedBy`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.persons: ~0 rows (приблизительно)
/*!40000 ALTER TABLE `persons` DISABLE KEYS */;
INSERT INTO `persons` (`ID`, `name`, `addedBy`) VALUES
	(1, 'Путин', 1);
/*!40000 ALTER TABLE `persons` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.personspagerank
CREATE TABLE IF NOT EXISTS `personspagerank` (
  `PersonID` int(11) NOT NULL,
  `PageID` int(11) NOT NULL,
  `Rank` int(11) DEFAULT NULL,
  PRIMARY KEY (`PersonID`,`PageID`),
  KEY `FK_personspagerank_pages` (`PageID`),
  KEY `FK_personspagerank_persons` (`PersonID`),
  CONSTRAINT `FK_personspagerank_pages` FOREIGN KEY (`PageID`) REFERENCES `pages` (`ID`),
  CONSTRAINT `FK_personspagerank_persons` FOREIGN KEY (`PersonID`) REFERENCES `persons` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.personspagerank: ~1 rows (приблизительно)
/*!40000 ALTER TABLE `personspagerank` DISABLE KEYS */;
INSERT INTO `personspagerank` (`PersonID`, `PageID`, `Rank`) VALUES
	(1, 1, NULL);
/*!40000 ALTER TABLE `personspagerank` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.sites
CREATE TABLE IF NOT EXISTS `sites` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(2048) NOT NULL,
  `addedBy` int(11) NOT NULL,
  `siteDescription` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK_sites_users` (`addedBy`),
  CONSTRAINT `FK_sites_users` FOREIGN KEY (`addedBy`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.sites: ~0 rows (приблизительно)
/*!40000 ALTER TABLE `sites` DISABLE KEYS */;
INSERT INTO `sites` (`ID`, `name`, `addedBy`, `siteDescription`) VALUES
	(1, 'lenta.ru', 1, 'Новостной сайт Лента.ру');
/*!40000 ALTER TABLE `sites` ENABLE KEYS */;

-- Дамп структуры для таблица searchandratewords.users
CREATE TABLE IF NOT EXISTS `users` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `parentID` int(11) DEFAULT NULL,
  `isAdmin` int(11) NOT NULL,
  `login` varchar(2048) NOT NULL,
  `password` varchar(2048) NOT NULL,
  `email` varchar(2048) NOT NULL,
  `token` varchar(1024) DEFAULT NULL,
  `tokenCreatedDate` datetime DEFAULT NULL,
  `tokenLastAccess` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `token` (`token`),
  KEY `FK_users_users` (`parentID`),
  CONSTRAINT `FK_users_users` FOREIGN KEY (`parentID`) REFERENCES `users` (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- Дамп данных таблицы searchandratewords.users: ~4 rows (приблизительно)
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`ID`, `parentID`, `isAdmin`, `login`, `password`, `email`, `token`, `tokenCreatedDate`, `tokenLastAccess`) VALUES
	(1, NULL, 1, 'Admin1', '1111', 'mail1@mail.com', '097f1565-ba48-4dcc-a2a1-fd3e15a93102', '2018-06-11 19:17:12', '2018-06-11 19:17:20'),
	(2, 1, 1, 'Admin2', '2222', 'mail2@mail.com', '097f1565-ba48-4dcc-a2a1-fd3e15a93103', '2018-06-11 19:17:15', '2018-06-11 19:17:22'),
	(3, 2, 0, 'User1', '3333', 'mail3@mail.com', '097f1565-ba48-4dcc-a2a1-fd3e15a93104', '2018-06-11 19:17:16', '2018-06-11 19:17:23'),
	(4, 1, 0, 'User2', '4444', 'mail4@mail.com', '097f1565-ba48-4dcc-a2a1-fd3e15a93105', '2018-06-11 19:17:18', '2018-06-11 19:17:24');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
