package com.ncr.capillary.PointDetails;

public class Response {
	private Status status = new Status();
	private Points points = new Points();

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Points getPoints() {
		return points;
	}

	public void setPoints(Points points) {
		this.points = points;
	}
}
