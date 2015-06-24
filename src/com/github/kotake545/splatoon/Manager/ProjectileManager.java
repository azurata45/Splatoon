package com.github.kotake545.splatoon.Manager;

import java.util.ArrayList;
import java.util.List;

import com.github.kotake545.splatoon.ProjectileInfo;

public class ProjectileManager {
	private static List<ProjectileInfo> projectiles;

	public ProjectileManager(){
		projectiles = new ArrayList<ProjectileInfo>();
	}

	public void tick(){
		for(int i = projectiles.size() - 1; i >= 0; i--){
			ProjectileInfo pi = projectiles.get(i);
			if(pi!=null){
				pi.tick();
			}
		}
	}

	public void remove(ProjectileInfo projectileInfo) {
		if(projectiles.contains(projectileInfo)){
			projectiles.remove(projectileInfo);
		}
	}

	public void addProjectileInfo(ProjectileInfo pi) {
		projectiles.add(pi);
	}

}
