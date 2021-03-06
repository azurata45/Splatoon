package com.github.kotake545.splatoon;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

import com.github.kotake545.splatoon.Packet.ParticleAPI;
import com.github.kotake545.splatoon.Packet.ParticleAPI.EnumParticle;
import com.github.kotake545.splatoon.util.BlockUtil;
import com.github.kotake545.splatoon.util.Utils;

public class ProjectileInfo {
	private Projectile projectile;
	private Vector vector;
	private Location startloc;
	private Location lastloc;
	private IkaPlayerInfo shooter;
	private IkaWeapon weapon;
	private Integer[] block;
	private double damage;

	private double shootDistance;
	private double moveFinishMultiply;
	private double hitRadius;
	private double shootKnockback;

	private int tick;
	private boolean dead = false;
	private boolean canmove = true;
	private boolean removed = false;

	private boolean destroyNextTick = false;

	private boolean afterbullet = false;


	public ProjectileInfo(IkaPlayerInfo shooter,IkaWeapon weapon,Vector vec,double percentage) {
		this.shooter = shooter;
		this.weapon = weapon;
		this.shootDistance = weapon.shootDistance*percentage;
		this.vector = vec;
		this.block = shooter.getTeamBlock();
		this.damage = weapon.shootDamage*percentage;
		this.projectile = shooter.getPlayer().launchProjectile(Snowball.class);
		projectile.setShooter(shooter.getPlayer());
		startloc = projectile.getLocation();
		lastloc = projectile.getLocation();
		this.moveFinishMultiply = weapon.moveFinishMultiply;
		this.hitRadius = weapon.hitRadius;
		this.shootKnockback = weapon.shootKnockback*percentage;
		this.afterbullet = weapon.paintBallistic;

	}

	public void tick(){
		if(!dead&&!projectile.isDead()){
			tick++;
			if(projectile != null){
				if(afterbullet&&canmove&&block!=null){
					/**
					 * 下を塗りつぶす処理
					 */
					for(Location location:AfterBullet(weapon.paintBallisticHeight)){
						for (Location bloc : BlockUtil.getPaintVerticalLine(location,null,1,weapon.paintBallisticDistance)){
							//ParticleAPI.sendAllPlayer(EnumParticle.BLOCK_DUST.setItemIDandData(block[0], block[1]),bloc,0.1F,0.1F,0.1F,0,3);
							bloc.setY(location.getY()-0.1);
							//ポイント追加
							setBlock(bloc,this.block);

							//ポイント追加
							Location clone = bloc.clone();
//							clone.setY(bloc.getY()-1);
//							setBlock(clone,this.block);
							clone.setY(bloc.getY()+1);
							setBlock(clone,this.block);
						}
					}
				}
				lastloc = projectile.getLocation();
				if(block!=null){
					ParticleAPI.sendAllPlayer(EnumParticle.BLOCK_DUST.setItemIDandData(block[0],block[1]),lastloc,0,0,0,0,1);
				}
				if(canmove){
					if(startloc.distance(lastloc)>shootDistance){
						canmove = false;
						vector.multiply(moveFinishMultiply);
					}
					projectile.setVelocity(vector);
				}
			}else{
				dead = true;
			}
		}else{
			dead=true;
		}
		if(dead){
			remove();
		}
		if(destroyNextTick){
			dead = true;
		}
	}

	public void setBlock(Location bloc,Integer[] block){
		if(Splatoon.blockUtil.setBlock(bloc,block)){
			shooter.point+=1;
		}
	}

	/**
	 * 弾道の下のブロックを y 以内でチェックする
	 * @param y
	 * @return
	 */
	public List<Location> AfterBullet(double y){
		List<Location> locations = new ArrayList<Location>();
		Location last = lastloc.clone();
		Location next = projectile.getLocation().clone();
		//弾道
		List<Location> A = BlockUtil.getLine(last,next);
		//弾道に-Y +addVectorでずらした弾道
		List<Location> B = new ArrayList<Location>();
		for(Location a:A){
			ParticleAPI.sendAllPlayer(EnumParticle.valueOf(weapon.ballistic),a,0,0,0,0,1);
			Location b= a.clone();
			b.setY(b.getY()-y);
			b.add(projectile.getVelocity());
			B.add(b);
		}
		for(int i = 0;i<A.size();i++){
			Location location = BlockUtil.getBetweenBlock(A.get(i),B.get(i));
			if(location!=null){
				locations.add(location);
			}
		}
		return locations;
	}


	public void remove(){
		dead = true;
		Splatoon.projectileManager.remove(this);
		projectile.remove();
		onHit(projectile.getLocation());
		onDestroy();
	}
	public void onHit(Location hit){
		if(removed){
			return;
		}
		removed = true;
		Integer[] a = shooter.getTeamBlock();
		if(a==null){
			return;
		}
		Location from = hit;
		if(from.getBlock().getTypeId() == 0){
			for(double i = 0.2D; i < 4.0D; i += 0.2D){
				if(from.getBlock().getTypeId() == 0){
					from = from.add(new Vector(0,-1,0).normalize().multiply(i));
				}
			}
		}
//		Splatoon.blockUtil.setBlock(from,a);
		double radius = hitRadius;
		ParticleAPI.sendAllPlayer(EnumParticle.BLOCK_DUST.setItemIDandData(a[0], a[1]),from,1F,0.2F,1F,0,5);
		for(String name:Splatoon.ikaConfig.shootPaintSound.keySet()){
			Utils.playSound(name,Splatoon.ikaConfig.shootPaintSound.get(name),from);
		}

		for (Location bloc : BlockUtil.getPaintSphere(from,radius)){
			//ポイント追加
			if(Splatoon.blockUtil.setBlock(bloc,a)){
				shooter.point+=1;
			}
		}
	}
	public void onDestroy(){
		projectile.remove();
		projectile = null;
		vector = null;
		weapon = null;
		shooter = null;
	}

	public void setDestroyNextTick(Boolean destroy){
		destroyNextTick = destroy;
	}

	public Projectile getProjectile() {
		return projectile;
	}

	public double getDamage(){
		return damage;
	}

	public void onKnockBack(Entity entity) {
		if(shootKnockback > 0.0D){
			Vector speed = vector;
			speed.normalize().setY(0.6D).multiply(shootKnockback / 4.0D);
			entity.setVelocity(speed);
		}
	}
}
