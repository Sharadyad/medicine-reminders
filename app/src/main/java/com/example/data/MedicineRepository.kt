package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {
    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()

    suspend fun getMedicineById(id: Long): Medicine? {
        return medicineDao.getMedicineById(id)
    }

    suspend fun insert(medicine: Medicine): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun delete(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
    }
}
