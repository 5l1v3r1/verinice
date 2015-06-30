package sernet.gs.reveng;

// Generated Jun 5, 2015 1:28:30 PM by Hibernate Tools 3.4.0.CR1

/**
 * MbMasGefId generated by hbm2java
 */
public class MbMasGefId implements java.io.Serializable {

	private int impId;
	private int gefImpId;
	private int gefId;
	private int masImpId;
	private int masId;

	public MbMasGefId() {
	}

	public MbMasGefId(int impId, int gefImpId, int gefId, int masImpId,
			int masId) {
		this.impId = impId;
		this.gefImpId = gefImpId;
		this.gefId = gefId;
		this.masImpId = masImpId;
		this.masId = masId;
	}

	public int getImpId() {
		return this.impId;
	}

	public void setImpId(int impId) {
		this.impId = impId;
	}

	public int getGefImpId() {
		return this.gefImpId;
	}

	public void setGefImpId(int gefImpId) {
		this.gefImpId = gefImpId;
	}

	public int getGefId() {
		return this.gefId;
	}

	public void setGefId(int gefId) {
		this.gefId = gefId;
	}

	public int getMasImpId() {
		return this.masImpId;
	}

	public void setMasImpId(int masImpId) {
		this.masImpId = masImpId;
	}

	public int getMasId() {
		return this.masId;
	}

	public void setMasId(int masId) {
		this.masId = masId;
	}

	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof MbMasGefId))
			return false;
		MbMasGefId castOther = (MbMasGefId) other;

		return (this.getImpId() == castOther.getImpId())
				&& (this.getGefImpId() == castOther.getGefImpId())
				&& (this.getGefId() == castOther.getGefId())
				&& (this.getMasImpId() == castOther.getMasImpId())
				&& (this.getMasId() == castOther.getMasId());
	}

	public int hashCode() {
		int result = 17;

		result = 37 * result + this.getImpId();
		result = 37 * result + this.getGefImpId();
		result = 37 * result + this.getGefId();
		result = 37 * result + this.getMasImpId();
		result = 37 * result + this.getMasId();
		return result;
	}

}