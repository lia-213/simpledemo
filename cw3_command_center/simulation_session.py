from datetime import timedelta

class SimulationSession:
	"""Simple time-block simulation session tracker.

	Tracks deliveries per drone as start/end time intervals. Each move is
	assumed to take 10 seconds (as used in the tests).
	"""

	MOVE_SECONDS = 10

	def __init__(self):
		self.deliveries = []  # list of dicts with keys: drone_id, dispatch_id, start_time, end_time, moves, description

	def register_delivery(self, drone_id, dispatch_id, start_time, total_moves, description=None):
		"""Register a delivery and return the computed end time.

		end_time = start_time + total_moves * MOVE_SECONDS
		"""
		duration = timedelta(seconds=total_moves * self.MOVE_SECONDS)
		end_time = start_time + duration

		self.deliveries.append({
			'drone_id': drone_id,
			'dispatch_id': dispatch_id,
			'start_time': start_time,
			'end_time': end_time,
			'moves': total_moves,
			'description': description,
		})

		return end_time

	def check_availability(self, drone_id, start_time, total_moves):
		"""Check if `drone_id` is available for a window starting at `start_time`.

		Returns (is_available: bool, next_time: datetime).
		If not available, `next_time` is the end time of the conflicting delivery.
		If available, `next_time` is the requested start_time (caller may use it).
		"""
		requested_end = start_time + timedelta(seconds=total_moves * self.MOVE_SECONDS)

		for d in self.deliveries:
			if d['drone_id'] != drone_id:
				continue
			# overlap if start < existing_end and existing_start <= requested_end
			if (start_time < d['end_time']) and (d['start_time'] <= requested_end):
				return False, d['end_time']

		return True, start_time

	def get_busy_drones(self, when_time):
		"""Return list of drone_ids that are busy at `when_time`."""
		busy = set()
		for d in self.deliveries:
			if d['start_time'] <= when_time < d['end_time']:
				busy.add(d['drone_id'])
		return list(busy)

	def get_session_summary(self):
		"""Return a summary dict with total deliveries, unique drones used and total flight time in minutes."""
		total_deliveries = len(self.deliveries)
		unique_drones = {d['drone_id'] for d in self.deliveries}
		total_seconds = sum((d['end_time'] - d['start_time']).total_seconds() for d in self.deliveries)

		return {
			'total_deliveries': total_deliveries,
			'unique_drones_used': len(unique_drones),
			'total_flight_time_minutes': total_seconds / 60.0,
		}


__all__ = ['SimulationSession']

# simulation_session.py removed — placeholder intentionally left empty
