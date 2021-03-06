/*******************************************************************************
 * Copyright (c) 2013 <Project SWG>
 * 
 * This File is part of NGECore2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Using NGEngine to work with NGECore2 is making a combined work based on NGEngine. 
 * Therefore all terms and conditions of the GNU Lesser General Public License cover the combination.
 ******************************************************************************/
package services.spawn;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.python.core.Py;

import resources.common.collidables.CollidableCircle;
import resources.datatables.Options;
import resources.datatables.PvpStatus;
import resources.objects.tangible.TangibleObject;
import services.ai.LairActor;
import engine.resources.scene.Planet;
import engine.resources.scene.Point3D;
import engine.resources.scene.Quaternion;
import main.NGECore;

public class SpawnService {

	private NGECore core;
	private Map<Planet, List<SpawnArea>> spawnAreas = new ConcurrentHashMap<Planet, List<SpawnArea>>();
	private Map<String, MobileTemplate> mobileTemplates = new ConcurrentHashMap<String, MobileTemplate>();
	private Map<String, LairGroupTemplate> lairGroupTemplates = new ConcurrentHashMap<String, LairGroupTemplate>();
	private Map<String, LairTemplate> lairTemplates = new ConcurrentHashMap<String, LairTemplate>();
	
	public SpawnService(NGECore core) {
		this.core = core;
		for(Planet planet : core.terrainService.getPlanetList()) {
			spawnAreas.put(planet, new ArrayList<SpawnArea>());
		}
	}	
	
	public void spawnCreature(String template, float x, float y, float z) {
		spawnCreature(template, new Point3D(x, y, z));
	}
	
	public void spawnCreature(String template, Point3D position) {
		
	}
	
	public void spawnLair(String lairSpawnTemplate, Planet planet, Point3D position, int level) {
		
		LairTemplate lairTemplate = lairTemplates.get(lairSpawnTemplate);
		if(lairTemplate == null)
			return;
		TangibleObject lairObject = (TangibleObject) core.objectService.createObject(lairTemplate.getLairCRC(), 0, planet, position, new Quaternion(1, 0, 0, 0));
		
		if(lairObject == null)
			return;
		
		lairObject.setOptionsBitmask(Options.ATTACKABLE);
		lairObject.setPvPBitmask(PvpStatus.Attackable);
		lairObject.setMaxDamage(1000 * level);
		
		LairActor lairActor = new LairActor(lairObject, lairTemplate.getMobileName());
		lairObject.setAttachment("AI", lairActor);
		
		core.simulationService.add(lairObject, position.x, position.z, true);
		
	}
	
	public void loadMobileTemplates() {
		
	}
	
	public void addMobileTemplate() {
		
	}
	
	public void loadLairTemplates() {
	    Path p = Paths.get("scripts/mobiles/lairs");
	    FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	        	core.scriptService.callScript("scripts/mobiles/lairs/", file.getFileName().toString().replace(".py", ""), "addTemplate", core);
	        	return FileVisitResult.CONTINUE;
	        }
	    };
        try {
			Files.walkFileTree(p, fv);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addLairTemplate(String name, String mobile, int mobileLimit, String lairCRC) {
		lairTemplates.put(name, new LairTemplate(name, mobile, mobileLimit, lairCRC));
	}
	
	public void loadLairGroups() {
	    Path p = Paths.get("scripts/mobiles/lairgroups");
	    FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	        	core.scriptService.callScript("scripts/mobiles/lairgroups/", file.getFileName().toString().replace(".py", ""), "addLairGroup", core);
	        	return FileVisitResult.CONTINUE;
	        }
	    };
        try {
			Files.walkFileTree(p, fv);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addLairGroup(String name, Vector<LairSpawnTemplate> lairSpawnTemplates) {
		lairGroupTemplates.put(name, new LairGroupTemplate(name, lairSpawnTemplates));
	}
		
	public void loadSpawnAreas() {
	    Path p = Paths.get("scripts/mobiles/spawnareas");
	    FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	        	core.scriptService.callScript("scripts/mobiles/spawnareas/", file.getFileName().toString().replace(".py", ""), "addSpawnArea", core);
	        	return FileVisitResult.CONTINUE;
	        }
	    };
        try {
			Files.walkFileTree(p, fv);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addLairSpawnArea(String lairGroup, float x, float z, float radius, String planetName) {
		LairGroupTemplate lairGroupTemplate = lairGroupTemplates.get(lairGroup);
		Planet planet = core.terrainService.getPlanetByName(planetName);
		if(lairGroupTemplate == null || planet == null)
			return;
		CollidableCircle collidableCircle = new CollidableCircle(new Point3D(x, 0, z), radius, planet);
		LairSpawnArea lairSpawnArea = new LairSpawnArea(planet, collidableCircle, lairGroupTemplate);
		spawnAreas.get(planet).add(lairSpawnArea);
		core.simulationService.addCollidable(collidableCircle, x, z);
	}

	
}
